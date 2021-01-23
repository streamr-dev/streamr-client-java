package com.streamr.client.protocol.message_layer

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingJson
import com.streamr.client.utils.Address
import spock.lang.Specification

/**
 * Useful methods for subclasses to use
 */
class StreamrSpecification extends Specification {
	private int seqNo = 0

	protected StreamMessage createMessage(Map content) {
		return createMessage(new Date().getTime(), seqNo++, null, null, TestingAddresses.PUBLISHER_ID, content)
	}

	protected StreamMessage createMessage(long timestamp, Map content) {
		return createMessage(timestamp, 0, null, null, TestingAddresses.PUBLISHER_ID, content)
	}

	protected StreamMessage createMessage(long timestamp = 0, long sequenceNumber = 0, Long previousTimestamp = null, Long previousSequenceNumber = null, Address publisherId = new Address("publisherId"), Map content = [:], String msgChainId = "msgChainId") {
		final String streamId = "streamId"
		final int streamPartition = 0
		final MessageId messageId = new MessageId.Builder()
                .withStreamId(streamId)
                .withStreamPartition(streamPartition)
                .withTimestamp(timestamp)
                .withSequenceNumber(sequenceNumber)
                .withPublisherId(publisherId)
                .withMsgChainId(msgChainId)
                .createMessageId()
		return new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef((previousTimestamp != null ? new MessageRef(previousTimestamp, previousSequenceNumber ?: 0) : null))
                .withSerializedContent(TestingJson.toJson(content))
                .createStreamMessage()
	}
}
