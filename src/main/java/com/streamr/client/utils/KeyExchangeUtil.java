package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;
import com.streamr.client.exceptions.InvalidGroupKeyResponseException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class KeyExchangeUtil {
    private static final Logger log = LogManager.getLogger();

    private final KeyStorage keyStorage;
    private final MessageCreationUtil messageCreationUtil;
    private final EncryptionUtil encryptionUtil;
    private final AddressValidityUtil addressValidityUtil;
    private final Function<StreamMessage, Void> publishFunction;
    private final SetGroupKeysFunction setGroupKeysFunction;


    public KeyExchangeUtil(KeyStorage keyStorage, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Function<StreamMessage, Void> publishFunction,
                           SetGroupKeysFunction setGroupKeysFunction) {
        this.keyStorage = keyStorage;
        this.messageCreationUtil = messageCreationUtil;
        this.encryptionUtil = encryptionUtil;
        this.addressValidityUtil = addressValidityUtil;
        this.publishFunction = publishFunction;
        this.setGroupKeysFunction = setGroupKeysFunction;
    }

    public void handleGroupKeyRequest(StreamMessage groupKeyRequest) {
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

        ArrayList<GroupKey> keys;
        if (content.containsKey("range")) {
            Map<String, Object> range = (Map<String, Object>) content.get("range");
            long start = (long) range.get("start");
            long end = (long) range.get("end");
            keys = keyStorage.getKeysBetween(streamId, new Date(start), new Date(end));
        } else {
            keys = new ArrayList<>();
            GroupKey latest = keyStorage.getLatestKey(streamId);
            if (latest != null) {
                keys.add(latest);
            }
        }
        if (keys.isEmpty()) {
            throw new InvalidGroupKeyRequestException("Received group key request for stream '" + streamId + "' but no group key is set");
        }
        ArrayList<GroupKey> encryptedGroupKeys = new ArrayList<>();
        for (GroupKey key: keys) {
            encryptedGroupKeys.add(key.getEncrypted((String) content.get("publicKey")));
        }
        StreamMessage response = messageCreationUtil.createGroupKeyResponse(subscriberId, streamId, encryptedGroupKeys);
        publishFunction.apply(response);
    }

    public void handleGroupKeyResponse(StreamMessage groupKeyResponse) {
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
        if (encryptionUtil == null) {
            throw new InvalidGroupKeyResponseException("Cannot decrypt group key response without the private key.");
        }
        ArrayList<GroupKey> decryptedKeys = new ArrayList<>();
        for (Map<String, Object> map: (ArrayList<Map<String, Object>>) content.get("keys")) {
            GroupKey decryptedKey = GroupKey.fromMap(map).getDecrypted(encryptionUtil);
            EncryptionUtil.validateGroupKey(decryptedKey.getGroupKeyHex());
            decryptedKeys.add(decryptedKey);
        }
        setGroupKeysFunction.apply(streamId, groupKeyResponse.getPublisherId(), decryptedKeys);
    }

    @FunctionalInterface
    public interface SetGroupKeysFunction {
        void apply(String streamId, String publisherId, ArrayList<GroupKey> keys);
    }
}
