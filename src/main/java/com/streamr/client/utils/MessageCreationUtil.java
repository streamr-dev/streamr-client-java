package com.streamr.client.utils;

import com.streamr.client.protocol.message_layer.*;
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType;
import com.streamr.client.rest.Stream;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageCreationUtil {

    private final String publisherId;
    private final String msgChainId;
    private final SigningUtil signingUtil;
    private final Map<String, SecretKey> groupKeys = new HashMap<>();

    private final HashMap<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

    private final HashMap<String, Integer> cachedHashes = new HashMap<>();

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil) {
        this(publisherId, signingUtil, new HashMap<>());
    }

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil, Map<String, String> groupKeysHex) {
        this.publisherId = publisherId;
        msgChainId = RandomStringUtils.randomAlphanumeric(20);
        this.signingUtil = signingUtil;
        for (String streamId: groupKeysHex.keySet()) {
            String groupKeyHex = groupKeysHex.get(streamId);
            EncryptionUtil.validateGroupKey(groupKeyHex);
            groupKeys.put(streamId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
        }
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        return createStreamMessage(stream, payload, timestamp, partitionKey, null);
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey, String groupKeyHex) {
        if (groupKeyHex != null) {
            EncryptionUtil.validateGroupKey(groupKeyHex);
        }

        EncryptionType encryptionType;
        String content;
        if (groupKeys.containsKey(stream.getId()) && groupKeyHex != null) {
            encryptionType = EncryptionType.NEW_KEY_AND_AES;
            byte[] groupKeyBytes = DatatypeConverter.parseHexBinary(groupKeyHex);
            byte[] payloadBytes = HttpUtils.mapAdapter.toJson(payload).getBytes(StandardCharsets.UTF_8);
            byte[] plaintext = new byte[groupKeyBytes.length + payloadBytes.length];
            System.arraycopy(groupKeyBytes, 0, plaintext, 0, groupKeyBytes.length);
            System.arraycopy(payloadBytes, 0, plaintext, groupKeyBytes.length, payloadBytes.length);
            content = EncryptionUtil.encrypt(plaintext, groupKeys.get(stream.getId()));
            groupKeys.put(stream.getId(), new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
        } else if (groupKeys.containsKey(stream.getId()) || groupKeyHex != null) {
            if (groupKeyHex != null) {
                groupKeys.put(stream.getId(), new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
            }
            encryptionType = EncryptionType.AES;
            content = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(payload).getBytes(StandardCharsets.UTF_8), groupKeys.get(stream.getId()));
        } else {
            encryptionType = EncryptionType.NONE;
            content = HttpUtils.mapAdapter.toJson(payload);
        }
        int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);
        String key = stream.getId() + streamPartition;

        long sequenceNumber = getNextSequenceNumber(key, timestamp.getTime());
        MessageID msgId = new MessageID(stream.getId(), streamPartition, timestamp.getTime(), sequenceNumber, publisherId, msgChainId);
        MessageRef prevRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV31(msgId, prevRef, StreamMessage.ContentType.CONTENT_TYPE_JSON,
                encryptionType, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null
        );

        refsPerStreamAndPartition.put(key, new MessageRef(timestamp.getTime(), sequenceNumber));
        if (signingUtil != null) {
            signingUtil.signStreamMessage(streamMessage);
        }
        return streamMessage;
    }

    private int hash(String partitionKey) {
        Integer hash = cachedHashes.get(partitionKey);
        if (hash == null) {
            byte[] bytes;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                bytes = md.digest(partitionKey.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            hash = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            cachedHashes.put(partitionKey, hash);
        }
        return hash;
    }

    private int getStreamPartition(int nbPartitions, String partitionKey) {
        if (nbPartitions == 0) {
            throw new Error("partitionCount is falsey!");
        } else if (nbPartitions == 1) {
            return 0;
        } else if (partitionKey != null) {
            int h = hash(partitionKey);
            return Math.abs(h) % nbPartitions;
        } else {
            return (int) Math.floor(Math.random() * nbPartitions);
        }
    }

    private long getNextSequenceNumber(String key, long timestamp) {
        MessageRef prev = refsPerStreamAndPartition.get(key);
        if (prev == null || prev.getTimestamp() != timestamp) {
            return 0L;
        }
        return prev.getSequenceNumber() + 1L;
    }
}
