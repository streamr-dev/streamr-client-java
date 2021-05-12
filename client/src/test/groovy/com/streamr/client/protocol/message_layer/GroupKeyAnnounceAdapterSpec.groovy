package com.streamr.client.protocol.message_layer

import com.streamr.client.stream.EncryptedGroupKey
import spock.lang.Specification

class GroupKeyAnnounceAdapterSpec extends Specification {
	static final EncryptedGroupKey key1 = new EncryptedGroupKey("groupKeyId1", "encrypted1", null)
	static final EncryptedGroupKey key2 = new EncryptedGroupKey("groupKeyId2", "encrypted2", null)


	def "serialization and deserialization"(String serializedMessage, GroupKeyAnnounce message) {
		expect:
		AbstractGroupKeyMessage.deserialize(serializedMessage, StreamMessage.MessageType.GROUP_KEY_ANNOUNCE) == message
		message.serialize() == serializedMessage

		where:
		serializedMessage | message
		'["streamId",[["groupKeyId1","'+key1.encryptedGroupKeyHex+'"],["groupKeyId2","'+key2.encryptedGroupKeyHex+'"]]]' | new GroupKeyAnnounce("streamId", [key1, key2])
	}

}
