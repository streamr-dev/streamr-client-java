package com.streamr.client.protocol

import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import spock.lang.Specification

/**
 * Useful methods for subclasses to use
 */
class StreamrSpecification extends Specification {
    private int seqNo = 0

    protected StreamMessage createMessage(Map content) {
        return createMessage(new Date().getTime(), seqNo++, null, null, "publisherId", content)
    }

    protected StreamMessage createMessage(long timestamp, Map content) {
        return createMessage(timestamp, 0, null, null, "publisherId", content)
    }

    protected StreamMessage createMessage(long timestamp = 0, long sequenceNumber = 0, Long previousTimestamp = null, Long previousSequenceNumber = null, String publisherId = "publisherId", Map content = [:]) {
        new StreamMessage(
                new MessageID("streamId", 0, timestamp, sequenceNumber, publisherId, "msgChainId"),
                (previousTimestamp != null ? new MessageRef(previousTimestamp, previousSequenceNumber ?: 0) : null),
                content
        )
    }
}
