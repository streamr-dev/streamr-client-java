package com.streamr.client.testing

import com.streamr.client.protocol.common.MessageRef

final class TestingMessageRef {
	private TestingMessageRef() {}

	public static MessageRef createMessageRef(final Long previousTimestamp, final Long previousSequenceNumber) {
		if (previousTimestamp != null) {
			Long sequenceNumber = 0L
			if (previousSequenceNumber != null) {
				sequenceNumber = previousSequenceNumber
			}
			return new MessageRef(previousTimestamp, sequenceNumber)
		}
		return null
	}
}
