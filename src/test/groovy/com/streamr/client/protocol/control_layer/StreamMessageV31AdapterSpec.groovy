package com.streamr.client.protocol.control_layer

import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageAdapter

class StreamMessageV31AdapterSpec extends StreamrSpecification {
	StreamMessageAdapter adapter
	StreamMessage msg

	void setup() {
		adapter = new StreamMessageAdapter()

		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		msg = new StreamMessage.Builder()
				.setMessageID(new MessageID("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, publisherId, "1"))
				.setPreviousMessageRef(new MessageRef(1528228170000L, 0))
				.setMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
				.setSerializedContent(serializedContent)
				.setContentType(StreamMessage.ContentType.JSON)
				.setEncryptionType(StreamMessage.EncryptionType.NONE)
				.setGroupKeyId(null)
				.setNewGroupKey(null)
				.setSignatureType(StreamMessage.SignatureType.ETH)
				.setSignature("signature")
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
		msg.getPublisherId() == publisherId
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef().getTimestamp() == 1528228170000L
		msg.getPreviousMessageRef().getTimestampAsDate() == new Date(1528228170000L)
		msg.getPreviousMessageRef().getSequenceNumber() == 0
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.ContentType.JSON
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
		msg.getPublisherId() == publisherId
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.ContentType.JSON
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
		msg.getPublisherId() == publisherId
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getMessageType() == StreamMessage.MessageType.STREAM_MESSAGE
		msg.getContentType() == StreamMessage.ContentType.JSON
		msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
		msg.getParsedContent() instanceof Map
		msg.getParsedContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.NONE
		msg.getSignature() == null
	}

}
