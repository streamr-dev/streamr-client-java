package com.streamr.client.utils;

import com.streamr.client.exceptions.*;
import com.streamr.client.protocol.message_layer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class KeyExchangeUtil {
    private static final Logger log = LoggerFactory.getLogger(KeyExchangeUtil.class);
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

    public static final String KEY_EXCHANGE_STREAM_PREFIX = "SYSTEM/keyexchange/";

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

    public void handleGroupKeyRequest(StreamMessage streamMessage) throws InvalidGroupKeyRequestException {
        // The StreamMessage should already be validated by the StreamrClient, but double-check
        if (streamMessage.getSignature() == null) {
            throw new InvalidGroupKeyRequestException("Received unsigned group key request (the public key must be signed to avoid MitM attacks).");
        }

        GroupKeyRequest request = (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

        String streamId = request.getStreamId();
        String sender = streamMessage.getPublisherId();
        if (!addressValidityUtil.isValidSubscriber(streamId, sender)) {
            throw new InvalidGroupKeyRequestException("Received group key request for stream '" + streamId + "' from invalid address '" + sender + "'");
        }

        GroupKeyRequest.Range range = request.getRange();
        ArrayList<GroupKey> keys;
        if (range != null) {
            log.debug("Querying keys for stream {} between {} and {}. Key storage content is {}",
                    streamId, range.getStart(), range.getEnd(), keyStorage.getKeysBetween(streamId, 0, new Date().getTime()));
            keys = keyStorage.getKeysBetween(streamId, range.getStart(), range.getEnd());
        } else {
            keys = new ArrayList<>();
            log.debug("Querying latest key for stream {}. Key storage content is {}",
                    streamId, keyStorage.getKeysBetween(streamId, 0, new Date().getTime()));
            GroupKey latest = keyStorage.getLatestKey(streamId);
            if (latest != null) {
                keys.add(latest);
            }
        }
        if (keys.isEmpty()) {
            throw new InvalidGroupKeyRequestException("Received group key request for stream '" + streamId + "' but no group key is set");
        }
        ArrayList<EncryptedGroupKey> encryptedGroupKeys = new ArrayList<>();
        RSAPublicKey publicKey;
        try {
            EncryptionUtil.validatePublicKey(request.getPublicKey());
            publicKey = EncryptionUtil.getPublicKeyFromString(request.getPublicKey());
        } catch (Exception e) {
            throw new InvalidGroupKeyRequestException(e.getMessage());
        }
        for (GroupKey key: keys) {
            encryptedGroupKeys.add(key.getEncrypted(publicKey));
        }
        publicKeys.put(sender, publicKey);
        StreamMessage response = messageCreationUtil.createGroupKeyResponse(sender, request, encryptedGroupKeys);
        publishFunction.accept(response);
    }

    public void handleGroupKeyResponse(StreamMessage streamMessage) throws InvalidGroupKeyResponseException {
        // The StreamMessage should already be validated by the StreamrClient, but double-check
        if (streamMessage.getSignature() == null) {
            throw new InvalidGroupKeyResponseException("Received unsigned group key response (it must be signed to avoid MitM attacks).");
        }

        GroupKeyResponse response = (GroupKeyResponse) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

        // A valid publisher of the client's inbox stream could send key responses for other streams to which
        // the publisher doesn't have write permissions. Thus the following additional check is necessary.
        if (!addressValidityUtil.isValidPublisher(response.getStreamId(), streamMessage.getPublisherId())) {
            throw new InvalidGroupKeyResponseException("Received group key from an invalid publisher "
                    + streamMessage.getPublisherId() + " for stream " + response.getStreamId());
        }
        ArrayList<GroupKey> decryptedKeys = new ArrayList<>();
        for (GroupKeyResponse.Key key : response.getKeys()) {
            try {
                GroupKey decryptedKey = new EncryptedGroupKey(key.getGroupKey(), new Date(key.getStart())).getDecrypted(encryptionUtil);
                decryptedKeys.add(decryptedKey);
            } catch (UnableToDecryptException | InvalidGroupKeyException e) {
                throw new InvalidGroupKeyResponseException(e.getMessage());
            }
        }
        try {
            log.debug("Received group key response for stream {}, keys {}",
                    response.getStreamId(), decryptedKeys);
            setGroupKeysFunction.apply(response.getStreamId(), streamMessage.getPublisherId(), decryptedKeys);
        } catch (UnableToSetKeysException e) {
            throw new InvalidGroupKeyResponseException(e.getMessage());
        }
    }

    public void handleGroupKeyReset(StreamMessage streamMessage) throws InvalidGroupKeyResetException {
        // The StreamMessage should already be validated by the StreamrClient, but double-check
        if (streamMessage.getSignature() == null) {
            throw new InvalidGroupKeyResetException("Received unsigned group key reset (it must be signed to avoid MitM attacks).");
        }

        GroupKeyAnnounce announce = (GroupKeyAnnounce) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);
        // A valid publisher of the client's inbox stream could send key resets for other streams to which
        // the publisher doesn't have write permissions. Thus the following additional check is necessary.
        if (!addressValidityUtil.isValidPublisher(announce.getStreamId(), streamMessage.getPublisherId())) {
            throw new InvalidGroupKeyResetException("Received group key reset from an invalid publisher "
                    + streamMessage.getPublisherId() + " for stream " + announce.getStreamId());
        }
        GroupKey newGroupKey;
        try {
            newGroupKey = new EncryptedGroupKey(announce.getGroupKey(), new Date(announce.getStart())).getDecrypted(encryptionUtil);
        } catch (UnableToDecryptException | InvalidGroupKeyException e) {
            throw new InvalidGroupKeyResetException(e.getMessage());
        }
        ArrayList<GroupKey> keyWrapped = new ArrayList<>();
        keyWrapped.add(newGroupKey);
        try {
            setGroupKeysFunction.apply(announce.getStreamId(), streamMessage.getPublisherId(), keyWrapped);
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
        GroupKey groupKeyReset = GroupKey.generate();
        Set<String> trueSubscribersSet = addressValidityUtil.getSubscribersSet(streamId, getSubscribersLocally);
        Set<String> revoked = new HashSet<>();
        for (String subscriberId: publicKeys.keySet() ) { // iterating over local cache of Ethereum address --> RSA public key
            if (trueSubscribersSet.contains(subscriberId)) { // if still valid subscriber, send the new key
                EncryptedGroupKey encryptedGroupKey = groupKeyReset.getEncrypted(publicKeys.get(subscriberId));
                StreamMessage groupKeyResetMsg = messageCreationUtil.createGroupKeyAnnounce(subscriberId, streamId, encryptedGroupKey);
                publishFunction.accept(groupKeyResetMsg);
            } else { // no longer a valid subscriber, to be removed from local cache
                revoked.add(subscriberId);
            }
        }
        revoked.forEach(publicKeys::remove); // remove all revoked (Ethereum address --> RSA public key) from local cache
        keyStorage.addKey(streamId, groupKeyReset);
    }

    public static String getKeyExchangeStreamId(String recipientAddress) {
        return KEY_EXCHANGE_STREAM_PREFIX + recipientAddress.toLowerCase();
    }

    public static boolean isKeyExchangeStreamId(String streamId) {
        return streamId.startsWith(KEY_EXCHANGE_STREAM_PREFIX);
    }

    public static String getRecipientFromKeyExchangeStreamId(String keyExchangeStreamId) {
        return keyExchangeStreamId.substring(KEY_EXCHANGE_STREAM_PREFIX.length());
    }

    @FunctionalInterface
    public interface SetGroupKeysFunction {
        void apply(String streamId, String publisherId, ArrayList<GroupKey> keys) throws UnableToSetKeysException;
    }
}
