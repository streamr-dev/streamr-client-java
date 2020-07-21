package com.streamr.client.protocol

import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.utils.Address
import spock.lang.Specification

/**
 * Useful methods for subclasses to use
 */
class StreamrSpecification extends Specification {
    private int seqNo = 0

    protected final Address subscriberId = new Address("subscriberId")
    protected final subscriberId1 = new Address("subscriberId1")
    protected final subscriberId2 = new Address("subscriberId2")
    protected final subscriberId3 = new Address("subscriberId3")
    protected final subscriberId4 = new Address("subscriberId4")
    protected final subscriberId5 = new Address("subscriberId5")

    protected final Address publisherId = new Address("publisherId")
    protected final publisherId1 = new Address("publisherId1")
    protected final publisherId2 = new Address("publisherId2")
    protected final publisherId3 = new Address("publisherId3")
    protected final publisherId4 = new Address("publisherId4")
    protected final publisherId5 = new Address("publisherId5")

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
