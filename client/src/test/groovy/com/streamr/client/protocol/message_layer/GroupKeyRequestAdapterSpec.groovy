package com.streamr.client.protocol.message_layer


import spock.lang.Specification

class GroupKeyRequestAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, GroupKeyRequest message) {
		expect:
		AbstractGroupKeyMessage.deserialize(serializedMessage, StreamMessage.MessageType.GROUP_KEY_REQUEST) == message
		message.serialize() == serializedMessage

		where:
		serializedMessage | message
		'["requestId","streamId","rsaPublicKey",["groupKey1","groupKey2"]]' | new GroupKeyRequest("requestId", "streamId", "rsaPublicKey", ["groupKey1", "groupKey2"])
	}

}
