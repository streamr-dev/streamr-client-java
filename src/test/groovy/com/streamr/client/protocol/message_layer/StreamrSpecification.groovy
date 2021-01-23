package com.streamr.client.protocol.message_layer

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.testing.TestingJson
import com.streamr.client.utils.Address
import spock.lang.Specification

/**
 * Useful methods for subclasses to use
 */
class StreamrSpecification extends Specification {
	private int seqNo = 0

	// Some reusable addresses for tests to use
	final String publisherPrivateKey = "d462a6f2ccd995a346a841d110e8c6954930a1c22851c0032d3116d8ccd2296a"
	final Address publisher = new Address("0x6807295093ac5da6fb2a10f7dedc5edd620804fb")
	final String subscriberPrivateKey = "81fe39ed83c4ab997f64564d0c5a630e34c621ad9bbe51ad2754fac575fc0c46"
	final Address subscriber = new Address("0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527")

	protected static final Address subscriberId = new Address("subscriberId")
	protected static final Address publisherId = new Address("publisherId")


	protected static Address getPublisherId(int number) {
		return new Address("publisherId${number}")
	}

	protected StreamMessage createMessage(Map content) {
		return createMessage(new Date().getTime(), seqNo++, null, null, publisherId, content)
	}

	protected StreamMessage createMessage(long timestamp, Map content) {
		return createMessage(timestamp, 0, null, null, publisherId, content)
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
