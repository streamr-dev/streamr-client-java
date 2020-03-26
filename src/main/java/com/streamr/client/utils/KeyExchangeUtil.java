package com.streamr.client.utils;

import com.streamr.client.exceptions.*;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class KeyExchangeUtil {
    private static final Logger log = LogManager.getLogger();
    private final Clock clock;
    public static final int REVOCATION_THRESHOLD = 5;
    public static final int REVOCATION_DELAY = 10; // in minutes

    private final KeyStorage keyStorage;
    private final MessageCreationUtil messageCreationUtil;
    private final EncryptionUtil encryptionUtil;
    private final AddressValidityUtil addressValidityUtil;
    private final Consumer<StreamMessage> publishFunction;
    private final SetGroupKeysFunction setGroupKeysFunction;
    private Instant lastCallToCheckRevocation = Instant.MIN;
    private final HashMap<String, RSAPublicKey> publicKeys = new HashMap<>();


    public KeyExchangeUtil(KeyStorage keyStorage, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Consumer<StreamMessage> publishFunction,
                           SetGroupKeysFunction setGroupKeysFunction) {
        this(keyStorage, messageCreationUtil, encryptionUtil, addressValidityUtil, publishFunction,
                setGroupKeysFunction, Clock.systemDefaultZone());
    }

    // constructor used for testing in KeyExchangeUtilSpec
    public KeyExchangeUtil(KeyStorage keyStorage, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Consumer<StreamMessage> publishFunction,
                           SetGroupKeysFunction setGroupKeysFunction, Clock clock) {
        this.keyStorage = keyStorage;
        this.messageCreationUtil = messageCreationUtil;
        this.encryptionUtil = encryptionUtil;
        this.addressValidityUtil = addressValidityUtil;
        this.publishFunction = publishFunction;
        this.setGroupKeysFunction = setGroupKeysFunction;
        this.clock = clock;
    }

    public void handleGroupKeyRequest(StreamMessage groupKeyRequest) throws InvalidGroupKeyRequestException {
        // if it was signed, the StreamrClient already checked the signature. If not, StreamrClient accepted it since the stream
        // does not require signed data for all types of messages.
        if (groupKeyRequest.getSignature() == null) {
            throw new InvalidGroupKeyRequestException("Received unsigned group key request (the public key must be signed to avoid MitM attacks).");
        }
        // No need to check if parsedContent contains the necessary fields because it was already checked during deserialization
        Map<String, Object> content;
        try {
            content = groupKeyRequest.getContent();
        } catch (IOException e) {
            log.error(e);
            return;
        }

        String streamId = (String) content.get("streamId");
        String subscriberId = groupKeyRequest.getPublisherId();
        if (!addressValidityUtil.isValidSubscriber(streamId, subscriberId)) {
            throw new InvalidGroupKeyRequestException("Received group key request for stream '" + streamId + "' from invalid address '" + subscriberId + "'");
        }

        ArrayList<UnencryptedGroupKey> keys;
        if (content.containsKey("range")) {
            Map<String, Object> range = (Map<String, Object>) content.get("range");
            // Need to use Double because Moshi parser converts all JSON numbers to double
            long start = ((Double) range.get("start")).longValue();
            long end = ((Double) range.get("end")).longValue();
            keys = keyStorage.getKeysBetween(streamId, start, end);
        } else {
            keys = new ArrayList<>();
            UnencryptedGroupKey latest = keyStorage.getLatestKey(streamId);
            if (latest != null) {
                keys.add(latest);
            }
        }
        if (keys.isEmpty()) {
            throw new InvalidGroupKeyRequestException("Received group key request for stream '" + streamId + "' but no group key is set");
        }
        ArrayList<EncryptedGroupKey> encryptedGroupKeys = new ArrayList<>();
        String publicKeyString = (String) content.get("publicKey");
        RSAPublicKey publicKey;
        try {
            EncryptionUtil.validatePublicKey(publicKeyString);
            publicKey = EncryptionUtil.getPublicKeyFromString(publicKeyString);
        } catch (Exception e) {
            throw new InvalidGroupKeyRequestException(e.getMessage());
        }
        for (UnencryptedGroupKey key: keys) {
            encryptedGroupKeys.add(key.getEncrypted(publicKey));
        }
        publicKeys.put(subscriberId, publicKey);
        StreamMessage response = messageCreationUtil.createGroupKeyResponse(subscriberId, streamId, encryptedGroupKeys);
        publishFunction.accept(response);
    }

    public void handleGroupKeyResponse(StreamMessage groupKeyResponse) throws InvalidGroupKeyResponseException {
        // if it was signed, the StreamrClient already checked the signature. If not, StreamrClient accepted it since the stream
        // does not require signed data for all types of messages.
        if (groupKeyResponse.getSignature() == null) {
            throw new InvalidGroupKeyResponseException("Received unsigned group key response (it must be signed to avoid MitM attacks).");
        }
        // No need to check if parsedContent contains the necessary fields because it was already checked during deserialization
        Map<String, Object> content;
        try {
            content = groupKeyResponse.getContent();
        } catch (IOException e) {
            log.error(e);
            return;
        }
        String streamId = (String) content.get("streamId");
        // A valid publisher of the client's inbox stream could send key responses for other streams to which
        // the publisher doesn't have write permissions. Thus the following additional check is necessary.
        if (!addressValidityUtil.isValidPublisher(streamId, groupKeyResponse.getPublisherId())) {
            throw new InvalidGroupKeyResponseException("Received group key from an invalid publisher "
                    + groupKeyResponse.getPublisherId() + " for stream " + streamId);
        }
        ArrayList<UnencryptedGroupKey> decryptedKeys = new ArrayList<>();
        for (Map<String, Object> map: (ArrayList<Map<String, Object>>) content.get("keys")) {
            try {
                UnencryptedGroupKey decryptedKey = EncryptedGroupKey.fromMap(map).getDecrypted(encryptionUtil);
                decryptedKeys.add(decryptedKey);
            } catch (UnableToDecryptException | InvalidGroupKeyException e) {
                throw new InvalidGroupKeyResponseException(e.getMessage());
            }
        }
        try {
            setGroupKeysFunction.apply(streamId, groupKeyResponse.getPublisherId(), decryptedKeys);
        } catch (UnableToSetKeysException e) {
            throw new InvalidGroupKeyResponseException(e.getMessage());
        }
    }

    public void handleGroupKeyReset(StreamMessage groupKeyReset) throws InvalidGroupKeyResetException {
        // if it was signed, the StreamrClient already checked the signature. If not, StreamrClient accepted it since the stream
        // does not require signed data for all types of messages.
        if (groupKeyReset.getSignature() == null) {
            throw new InvalidGroupKeyResetException("Received unsigned group key reset (it must be signed to avoid MitM attacks).");
        }
        // No need to check if parsedContent contains the necessary fields because it was already checked during deserialization
        Map<String, Object> content;
        try {
            content = groupKeyReset.getContent();
        } catch (IOException e) {
            log.error(e);
            return;
        }
        String streamId = (String) content.get("streamId");
        // A valid publisher of the client's inbox stream could send key resets for other streams to which
        // the publisher doesn't have write permissions. Thus the following additional check is necessary.
        if (!addressValidityUtil.isValidPublisher(streamId, groupKeyReset.getPublisherId())) {
            throw new InvalidGroupKeyResetException("Received group key reset from an invalid publisher "
                    + groupKeyReset.getPublisherId() + " for stream " + streamId);
        }
        UnencryptedGroupKey newGroupKey;
        try {
            newGroupKey = EncryptedGroupKey.fromMap(content).getDecrypted(encryptionUtil);
        } catch (UnableToDecryptException | InvalidGroupKeyException e) {
            throw new InvalidGroupKeyResetException(e.getMessage());
        }
        ArrayList<UnencryptedGroupKey> keyWrapped = new ArrayList<>();
        keyWrapped.add(newGroupKey);
        try {
            setGroupKeysFunction.apply(streamId, groupKeyReset.getPublisherId(), keyWrapped);
        } catch (UnableToSetKeysException e) {
            throw new InvalidGroupKeyResetException(e.getMessage());
        }
    }

    public boolean keyRevocationNeeded(String streamId) {
        Instant now = clock.instant();
        boolean res = false;
        if (lastCallToCheckRevocation.plus(Duration.ofMinutes(REVOCATION_DELAY)).isBefore(now)) {
            res = addressValidityUtil.nbSubscribersToRevoke(streamId) >= REVOCATION_THRESHOLD;
        }
        lastCallToCheckRevocation = now;
        return res;
    }

    public void rekey(String streamId, boolean getSubscribersLocally) {
        UnencryptedGroupKey groupKeyReset = EncryptionUtil.genGroupKey();
        Set<String> trueSubscribersSet = addressValidityUtil.getSubscribersSet(streamId, getSubscribersLocally);
        Set<String> revoked = new HashSet<>();
        for (String subscriberId: publicKeys.keySet() ) { // iterating over local cache of Ethereum address --> RSA public key
            if (trueSubscribersSet.contains(subscriberId)) { // if still valid subscriber, send the new key
                EncryptedGroupKey encryptedGroupKey = groupKeyReset.getEncrypted(publicKeys.get(subscriberId));
                StreamMessage groupKeyResetMsg = messageCreationUtil.createGroupKeyReset(subscriberId, streamId, encryptedGroupKey);
                publishFunction.accept(groupKeyResetMsg);
            } else { // no longer a valid subscriber, to be removed from local cache
                revoked.add(subscriberId);
            }
        }
        revoked.forEach(publicKeys::remove); // remove all revoked (Ethereum address --> RSA public key) from local cache
        keyStorage.addKey(streamId, groupKeyReset);
    }

    @FunctionalInterface
    public interface SetGroupKeysFunction {
        void apply(String streamId, String publisherId, ArrayList<UnencryptedGroupKey> keys) throws UnableToSetKeysException;
    }
}
