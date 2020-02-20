package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;
import com.streamr.client.exceptions.InvalidGroupKeyResponseException;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.SigningRequiredException;
import com.streamr.client.protocol.message_layer.*;
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType;
import com.streamr.client.rest.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

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

    //TODO: change back to private after testing
    public final String publisherId;
    private final String msgChainId;
    private final SigningUtil signingUtil;
    private final KeyStorage keyStorage;

    private final HashMap<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

    private final HashMap<String, Integer> cachedHashes = new HashMap<>();

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil, KeyStorage keyStorage) {
        this.publisherId = publisherId;
        msgChainId = RandomStringUtils.randomAlphanumeric(20);
        this.signingUtil = signingUtil;
        this.keyStorage = keyStorage;
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        return createStreamMessage(stream, payload, timestamp, partitionKey, null);
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey, UnencryptedGroupKey newGroupKey) {
        String groupKeyHex = newGroupKey == null ? null : newGroupKey.getGroupKeyHex();

        int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);

        Pair<MessageID, MessageRef> pair = createMsgIdAndRef(stream.getId(), streamPartition, timestamp.getTime());

        StreamMessage streamMessage = new StreamMessageV31(pair.getLeft(), pair.getRight(), StreamMessage.ContentType.CONTENT_TYPE_JSON,
                EncryptionType.NONE, payload, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null
        );

        if (keyStorage.hasKey(stream.getId()) && groupKeyHex != null) {
            EncryptionUtil.encryptStreamMessageAndNewKey(groupKeyHex, streamMessage, keyStorage.getLatestKey(stream.getId()).getSecretKey());
            keyStorage.addKey(stream.getId(), newGroupKey);
        } else if (keyStorage.hasKey(stream.getId()) || groupKeyHex != null) {
            if (groupKeyHex != null) {
                keyStorage.addKey(stream.getId(), newGroupKey);
            }
            EncryptionUtil.encryptStreamMessage(streamMessage, keyStorage.getLatestKey(stream.getId()).getSecretKey());
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

        Pair<MessageID, MessageRef> pair = createDefaultMsgIdAndRef(publisherAddress.toLowerCase()); // using address as streamId (inbox stream)
        StreamMessage streamMessage = new StreamMessageV31(
                pair.getLeft(), pair.getRight(), StreamMessage.ContentType.GROUP_KEY_REQUEST, EncryptionType.NONE, data,
                StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
        signingUtil.signStreamMessage(streamMessage);
        return streamMessage;
    }

    public StreamMessage createGroupKeyResponse(String subscriberAddress, String streamId, List<EncryptedGroupKey> encryptedGroupKeys) {
        if (signingUtil == null) {
            throw new SigningRequiredException("Cannot create unsigned group key response. Must authenticate with an Ethereum account");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("streamId", streamId);
        data.put("keys", encryptedGroupKeys.stream().map(GroupKey::toMap).collect(Collectors.toList()));

        Pair<MessageID, MessageRef> pair = createDefaultMsgIdAndRef(subscriberAddress.toLowerCase()); // using address as streamId (inbox stream)
        StreamMessage streamMessage = new StreamMessageV31(
                pair.getLeft(), pair.getRight(), StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE, EncryptionType.RSA, data,
                StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
        signingUtil.signStreamMessage(streamMessage);
        return streamMessage;
    }

    public StreamMessage createErrorMessage(String destinationAddress, Exception e) {
        if (signingUtil == null) {
            throw new SigningRequiredException("Cannot create unsigned error message. Must authenticate with an Ethereum account");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("code", getErrorCodeFromException(e));
        data.put("message", e.getMessage());
        Pair<MessageID, MessageRef> pair = createDefaultMsgIdAndRef(destinationAddress.toLowerCase()); // using address as streamId (inbox stream)
        StreamMessage streamMessage = new StreamMessageV31(
                pair.getLeft(), pair.getRight(), StreamMessage.ContentType.ERROR_MSG, EncryptionType.NONE, data,
                StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
        signingUtil.signStreamMessage(streamMessage);
        return streamMessage;
    }

    private String getErrorCodeFromException(Exception e) {
        if (e instanceof InvalidGroupKeyRequestException) {
            return "INVALID_GROUP_KEY_REQUEST";
        } else if (e instanceof InvalidGroupKeyResponseException) {
            return "INVALID_GROUP_KEY_RESPONSE";
        } else if (e instanceof MalformedMessageException) {
            return "INVALID_CONTENT_TYPE";
        } else {
            return "UNEXPECTED_ERROR";
        }
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

    private Pair<MessageID, MessageRef> createMsgIdAndRef(String streamId, int streamPartition, long timestamp) {
        String key = streamId + streamPartition;
        long sequenceNumber = getNextSequenceNumber(key, timestamp);
        MessageID msgId = new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
        MessageRef prevMsgRef = refsPerStreamAndPartition.get(key);
        Pair<MessageID, MessageRef> p = Pair.of(msgId, prevMsgRef);
        refsPerStreamAndPartition.put(key, new MessageRef(timestamp, sequenceNumber));
        return p;
    }

    private Pair<MessageID, MessageRef> createDefaultMsgIdAndRef(String streamId) {
        return createMsgIdAndRef(streamId, 0, (new Date()).getTime());
    }

    private long getNextSequenceNumber(String key, long timestamp) {
        MessageRef prev = refsPerStreamAndPartition.get(key);
        if (prev == null || prev.getTimestamp() != timestamp) {
            return 0L;
        }
        return prev.getSequenceNumber() + 1L;
    }
}
