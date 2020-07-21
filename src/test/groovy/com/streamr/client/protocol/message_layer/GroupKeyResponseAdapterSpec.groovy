package com.streamr.client.protocol.message_layer

import com.streamr.client.utils.GroupKey
import spock.lang.Specification

class GroupKeyResponseAdapterSpec extends Specification {
	static final GroupKey key1 = GroupKey.generate("groupKeyId1")
	static final GroupKey key2 = GroupKey.generate("groupKeyId2")

	def "serialization and deserialization"(String serializedMessage, GroupKeyResponse message) {
		expect:
		AbstractGroupKeyMessage.deserialize(serializedMessage, StreamMessage.MessageType.GROUP_KEY_RESPONSE) == message
		message.serialize() == serializedMessage

		where:
		serializedMessage | message
		'["requestId","streamId",[["groupKeyId1","'+key1.groupKeyHex+'"],["groupKeyId2","'+key2.groupKeyHex+'"]]]' | new GroupKeyResponse("requestId", "streamId", [key1, key2])
	}

}
