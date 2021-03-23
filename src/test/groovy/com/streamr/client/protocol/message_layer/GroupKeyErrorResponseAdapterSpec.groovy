package com.streamr.client.protocol.message_layer


import spock.lang.Specification

class GroupKeyErrorResponseAdapterSpec extends Specification {
	def "serialization and deserialization"(String serializedMessage, GroupKeyErrorResponse message) {
		expect:
		AbstractGroupKeyMessage.deserialize(serializedMessage, StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE) == message
		message.serialize() == serializedMessage

		where:
		serializedMessage | message
		'["requestId","streamId","errorCode","errorMessage",["groupKey1","groupKey2"]]' | new GroupKeyErrorResponse("requestId", "streamId", "errorCode", "errorMessage", ["groupKey1", "groupKey2"])
	}
}
