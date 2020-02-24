package com.streamr.client.utils;

import com.streamr.client.exceptions.*;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class KeyExchangeUtil {
    private static final Logger log = LogManager.getLogger();

    private final KeyStorage keyStorage;
    private final MessageCreationUtil messageCreationUtil;
    private final EncryptionUtil encryptionUtil;
    private final AddressValidityUtil addressValidityUtil;
    private final Consumer<StreamMessage> publishFunction;
    private final SetGroupKeysFunction setGroupKeysFunction;


    public KeyExchangeUtil(KeyStorage keyStorage, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Consumer<StreamMessage> publishFunction,
                           SetGroupKeysFunction setGroupKeysFunction) {
        this.keyStorage = keyStorage;
        this.messageCreationUtil = messageCreationUtil;
        this.encryptionUtil = encryptionUtil;
        this.addressValidityUtil = addressValidityUtil;
        this.publishFunction = publishFunction;
        this.setGroupKeysFunction = setGroupKeysFunction;
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
        setGroupKeysFunction.apply(streamId, groupKeyResponse.getPublisherId(), decryptedKeys);
    }

    @FunctionalInterface
    public interface SetGroupKeysFunction {
        void apply(String streamId, String publisherId, ArrayList<UnencryptedGroupKey> keys) throws InvalidGroupKeyResponseException;
    }
}