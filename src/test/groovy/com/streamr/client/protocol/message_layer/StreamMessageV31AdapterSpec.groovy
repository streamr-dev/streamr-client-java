package com.streamr.client.protocol.message_layer

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.testing.TestingAddresses
import spock.lang.Specification

class StreamMessageV31AdapterSpec extends Specification {
	StreamMessageAdapter adapter
	StreamMessage msg

	void setup() {
		adapter = new StreamMessageAdapter()

		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		final MessageId messageId = new MessageId.Builder()
				.withStreamId("7wa7APtlTq6EC5iTCBy6dw")
				.withStreamPartition(0)
				.withTimestamp(1528228173462L)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("1")
				.createMessageId()
		msg = new StreamMessage.Builder()
				.withMessageId(messageId)
				.withPreviousMessageRef(new MessageRef(1528228170000L, 0))
				.withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
				.withContent(StreamMessage.Content.Factory.withJsonAsPayload(serializedContent))
				.withEncryptionType(StreamMessage.EncryptionType.NONE)
				.withGroupKeyId(null)
				.withNewGroupKey(null)
				.withSignatureType(StreamMessage.SignatureType.ETH)
				.withSignature("signature")
				.createStreamMessage()
	}

	void "deserialize"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef().getTimestamp() == 1528228170000L
		msg.getPreviousMessageRef().getTimestampAsDate() == new Date(1528228170000L)
		msg.getPreviousMessageRef().getSequenceNumber() == 0
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.Content.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getParsedContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.ETH
		msg.getSignature() == "signature"
	}

	void "deserialize with previousMessageRef null"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.Content.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getParsedContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.ETH
		msg.getSignature() == "signature"
	}

	void "deserialize with no signature"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		msg = StreamMessageAdapter.deserialize(json)

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.Content.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getParsedContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.NONE
		msg.getSignature() == null
	}

}
