package com.streamr.client.protocol.message_layer

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.utils.EncryptedGroupKey
import spock.lang.Specification

class StreamMessageV32AdapterSpec extends Specification {
	private static final int VERSION = 32

	StreamMessageAdapter adapter
	StreamMessage msg

	void setup() {
		adapter = new StreamMessageAdapter()

		// Message with minimal fields
		final MessageId messageId = new MessageId.Builder()
				.withStreamId("streamId")
				.withStreamPartition(0)
				.withTimestamp(123L)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChainId")
				.createMessageId()
		msg = new StreamMessage.Builder()
				.withMessageId(messageId)
				.withPreviousMessageRef(null)
				.withContent(TestingContent.emptyMessage())
				.createStreamMessage()
	}

	void "serialize minimal message"() {
		String expectedJson = '[32,["streamId",0,123,0,"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","msgChainId"],null,27,0,0,null,"{}",null,0,null]'

		expect:
		adapter.serialize(msg, VERSION) == expectedJson
		adapter.deserialize(adapter.serialize(msg, VERSION)) == msg
	}

	void "serialize maximal message"() {
		String expectedJson = '[32,["streamId",0,123,0,"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","msgChainId"],[122,0],27,0,2,"groupKeyId","encrypted-content","[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex-cached\\\"]",2,"signature"]'
		msg = new StreamMessage.Builder(msg)
				.withSignature("signature")
				.withSignatureType(StreamMessage.SignatureType.ETH)
				.withContent(TestingContent.fromJsonString("encrypted-content"))
				.withPreviousMessageRef(new MessageRef(122L, 0))
				.withEncryptionType(StreamMessage.EncryptionType.AES)
				.withGroupKeyId("groupKeyId")
				.withNewGroupKey(new EncryptedGroupKey("newGroupKeyId", "encryptedGroupKeyHex", "[\"newGroupKeyId\",\"encryptedGroupKeyHex-cached\"]"))
				.createStreamMessage()

		expect:
		adapter.serialize(msg, VERSION) == expectedJson
		adapter.deserialize(adapter.serialize(msg, VERSION)) == msg
	}

	void "deserialize minimal message"() {
		String json = '[32,["streamId",0,123,0,"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","msgChainId"],null,27,0,0,null,"{}",null,0,null]'

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 123L
		msg.getTimestampAsDate() == new Date(123L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
		msg.getMsgChainId() == "msgChainId"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.Content.Type.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getSignatureType() == StreamMessage.SignatureType.NONE
		msg.getSignature() == null

		adapter.serialize(msg, VERSION) == json
	}

	void "deserialize maximal message"() {
		String json = '[32,["streamId",0,123,0,"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","msgChainId"],[122,0],27,0,2,"groupKeyId","encrypted-content","[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex\\\"]",2,"signature"]'

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 123L
		msg.getTimestampAsDate() == new Date(123L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
		msg.getMsgChainId() == "msgChainId"
		msg.getPreviousMessageRef() == new MessageRef(122L, 0)
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.Content.Type.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.AES
		msg.getSerializedContent() == "encrypted-content"
		msg.getNewGroupKey() == new EncryptedGroupKey("newGroupKeyId", "encryptedGroupKeyHex")
		msg.getSignatureType() == StreamMessage.SignatureType.ETH
		msg.getSignature() == "signature"

		adapter.serialize(msg, VERSION) == json
	}

}
