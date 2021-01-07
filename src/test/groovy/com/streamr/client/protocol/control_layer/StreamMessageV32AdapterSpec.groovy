package com.streamr.client.protocol.control_layer

import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageAdapter
import com.streamr.client.utils.EncryptedGroupKey
import com.streamr.client.utils.HttpUtils

class StreamMessageV32AdapterSpec extends StreamrSpecification {
	private static final int VERSION = 32

	StreamMessageAdapter adapter
	StreamMessage msg

	void setup() {
		adapter = new StreamMessageAdapter()

		// Message with minimal fields
		msg = new StreamMessage.Builder().setMessageID(new MessageID("streamId", 0, 123L, 0, publisherId, "msgChainId")).setPreviousMessageRef(null).setSerializedContent(HttpUtils.mapAdapter.toJson([:])).createStreamMessage()
	}

	void "serialize minimal message"() {
		String expectedJson = '[32,["streamId",0,123,0,"publisherid","msgChainId"],null,27,0,0,null,"{}",null,0,null]'

		expect:
		adapter.serialize(msg, VERSION) == expectedJson
		adapter.deserialize(adapter.serialize(msg, VERSION)) == msg
	}

	void "serialize maximal message"() {
		String expectedJson = '[32,["streamId",0,123,0,"publisherid","msgChainId"],[122,0],27,0,2,"groupKeyId","encrypted-content","[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex-cached\\\"]",2,"signature"]'
		msg.setPreviousMessageRef(new MessageRef(122L, 0))
		msg.setEncryptionType(StreamMessage.EncryptionType.AES);
		msg.setGroupKeyId("groupKeyId")
		msg.setNewGroupKey(new EncryptedGroupKey("newGroupKeyId", "encryptedGroupKeyHex", "[\"newGroupKeyId\",\"encryptedGroupKeyHex-cached\"]"))
		msg = new StreamMessage.Builder(msg).setSignature("signature").setSignatureType(StreamMessage.SignatureType.ETH)
				.setSerializedContent("encrypted-content")
				.createStreamMessage()

		expect:
		adapter.serialize(msg, VERSION) == expectedJson
		adapter.deserialize(adapter.serialize(msg, VERSION)) == msg
	}

	void "deserialize minimal message"() {
		String json = '[32,["streamId",0,123,0,"publisherid","msgChainId"],null,27,0,0,null,"{}",null,0,null]'

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 123L
		msg.getTimestampAsDate() == new Date(123L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == publisherId
		msg.getMsgChainId() == "msgChainId"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getSignatureType() == StreamMessage.SignatureType.NONE
		msg.getSignature() == null

		adapter.serialize(msg, VERSION) == json
	}

	void "deserialize maximal message"() {
		String json = '[32,["streamId",0,123,0,"publisherid","msgChainId"],[122,0],27,0,2,"groupKeyId","encrypted-content","[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex\\\"]",2,"signature"]'

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 123L
		msg.getTimestampAsDate() == new Date(123L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == publisherId
		msg.getMsgChainId() == "msgChainId"
		msg.getPreviousMessageRef() == new MessageRef(122L, 0)
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.AES
		msg.getSerializedContent() == "encrypted-content"
		msg.getNewGroupKey() == new EncryptedGroupKey("newGroupKeyId", "encryptedGroupKeyHex")
		msg.getSignatureType() == StreamMessage.SignatureType.ETH
		msg.getSignature() == "signature"

		adapter.serialize(msg, VERSION) == json
	}

}
