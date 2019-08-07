package com.streamr.client.utils;

import com.streamr.client.exceptions.SigningRequiredException;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageCreationUtil {

    private final String publisherId;
    private final String msgChainId;
    private final SigningUtil signingUtil;
    private final Map<String, SecretKey> groupKeys = new HashMap<>(); // streamId --> groupKey

    private final HashMap<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

    private final HashMap<String, Integer> cachedHashes = new HashMap<>();

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil) {
        this(publisherId, signingUtil, new HashMap<>());
    }

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil, Map<String, GroupKey> groupKeys) {
        this.publisherId = publisherId;
        msgChainId = RandomStringUtils.randomAlphanumeric(20);
        this.signingUtil = signingUtil;
        for (String streamId: groupKeys.keySet()) {
            String groupKeyHex = groupKeys.get(streamId).getGroupKeyHex();
            EncryptionUtil.validateGroupKey(groupKeyHex);
            this.groupKeys.put(streamId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
        }
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        return createStreamMessage(stream, payload, timestamp, partitionKey, null);
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey, GroupKey groupKey) {
        if (groupKey != null) {
            EncryptionUtil.validateGroupKey(groupKey.getGroupKeyHex());
        }
        String groupKeyHex = groupKey == null ? null : groupKey.getGroupKeyHex();

        int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);
        String key = stream.getId() + streamPartition;

        long sequenceNumber = getNextSequenceNumber(key, timestamp.getTime());
        MessageID msgId = new MessageID(stream.getId(), streamPartition, timestamp.getTime(), sequenceNumber, publisherId, msgChainId);
        MessageRef prevRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV31(msgId, prevRef, StreamMessage.ContentType.CONTENT_TYPE_JSON,
                EncryptionType.NONE, payload, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null
        );

        refsPerStreamAndPartition.put(key, new MessageRef(timestamp.getTime(), sequenceNumber));

        if (groupKeys.containsKey(stream.getId()) && groupKeyHex != null) {
            EncryptionUtil.encryptStreamMessageAndNewKey(groupKeyHex, streamMessage, groupKeys.get(stream.getId()));
            groupKeys.put(stream.getId(), new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
        } else if (groupKeys.containsKey(stream.getId()) || groupKeyHex != null) {
            if (groupKeyHex != null) {
                groupKeys.put(stream.getId(), new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
            }
            EncryptionUtil.encryptStreamMessage(streamMessage, groupKeys.get(stream.getId()));
        }

        if (signingUtil != null) {
            signingUtil.signStreamMessage(streamMessage);
        }
        return streamMessage;
    }

    public StreamMessage createGroupKeyRequest(String publisherAddress, String streamId, String rsaPublicKey, Date start, Date end) {
        if (signingUtil == null) {
            throw new SigningRequiredException("Cannot create unsigned group key request. Must authenticate with an Ethereum account");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("streamId", streamId);
        data.put("publicKey", rsaPublicKey);
        if (start != null && end != null) {
            Map<String, Long> range = new HashMap<>();
            range.put("start", start.getTime());
            range.put("end", end.getTime());
            data.put("range", range);
        }

        String key = publisherAddress + "0"; // streamId + streamPartition
        long timestamp = (new Date()).getTime();
        long sequenceNumber = getNextSequenceNumber(key, timestamp);
        MessageID msgId = new MessageID(publisherAddress, 0, timestamp, sequenceNumber, publisherId, msgChainId);
        MessageRef prevMsgRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV31(
                msgId, prevMsgRef, StreamMessage.ContentType.GROUP_KEY_REQUEST, EncryptionType.NONE, data,
                StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
        signingUtil.signStreamMessage(streamMessage);
        return streamMessage;
    }

    public StreamMessage createGroupKeyResponse(String subscriberAddress, String streamId, List<GroupKey> encryptedGroupKeys) {
        if (signingUtil == null) {
            throw new SigningRequiredException("Cannot create unsigned group key response. Must authenticate with an Ethereum account");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("streamId", streamId);
        data.put("keys", encryptedGroupKeys.stream().map(k -> k.toMap()).collect(Collectors.toList()));

        String key = subscriberAddress + "0"; // streamId + streamPartition
        long timestamp = (new Date()).getTime();
        long sequenceNumber = getNextSequenceNumber(key, timestamp);
        MessageID msgId = new MessageID(subscriberAddress, 0, timestamp, sequenceNumber, publisherId, msgChainId);
        MessageRef prevMsgRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV31(
                msgId, prevMsgRef, StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE, EncryptionType.RSA, data,
                StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
        signingUtil.signStreamMessage(streamMessage);
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
