package com.streamr.client;

import com.streamr.client.protocol.message_layer.MessageID;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageV30;
import com.streamr.client.rest.Stream;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageCreationUtil {
    private final String publisherId;
    private final String msgChainId;

    private final HashMap<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

    public MessageCreationUtil(String publisherId) {
        this.publisherId = publisherId;
        msgChainId = RandomStringUtils.randomAlphanumeric(20);
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);
        String key = stream.getId()+streamPartition;

        long sequenceNumber = getNextSequenceNumber(key, timestamp.getTime());
        MessageID msgId = new MessageID(stream.getId(), streamPartition, timestamp.getTime(), sequenceNumber, publisherId, msgChainId);
        MessageRef prevRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV30(msgId, prevRef, StreamMessage.ContentType.CONTENT_TYPE_JSON,
                payload, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null
        );

        refsPerStreamAndPartition.put(key, new MessageRef(timestamp.getTime(), sequenceNumber));
        return streamMessage;
    }

    private int getStreamPartition(int nbPartitions, String partitionKey) {
        if (nbPartitions == 0) {
            throw new Error("partitionCount is falsey!");
        } else if (nbPartitions == 1) {
            return 0;
        } else if (partitionKey != null) {
            int hash = partitionKey.hashCode();
            return Math.abs(hash) % nbPartitions;
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
