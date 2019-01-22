package com.streamr.client.protocol

import com.streamr.client.exceptions.MalformedMessageException
import com.streamr.client.exceptions.UnsupportedMessageException
import spock.lang.Specification
import com.streamr.client.protocol.message_layer.*
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType


class StreamMessageSpec extends Specification {
	String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
	void "toJson (v28)"() {
		String expectedJson = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"
		when:
		StreamMessageV28 msg = new StreamMessageV28("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, 1871084066, 1871084061, ContentType.CONTENT_TYPE_JSON, serializedContent)

		then:
		msg.toJson() == expectedJson
	}

	void "toJson (v29)"() {
		String expectedJson = "[29,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"publisherAddress\",\"signature\"]"

		when:
		StreamMessageV29 msg = new StreamMessageV29("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, 1871084066, 1871084061, ContentType.CONTENT_TYPE_JSON, serializedContent, SignatureType.SIGNATURE_TYPE_ETH, "publisherAddress", "signature")

		then:
		msg.toJson() == expectedJson
	}

	void "toJson (v30)"() {
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"

		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, SignatureType.SIGNATURE_TYPE_ETH, "signature")

		then:
		msg.toJson() == expectedJson
	}

	void "toJson with no signature (v30)"() {
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msg.toJson() == expectedJson
	}

	void "toJson with null previous message ref (v30)"() {
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", (Long) null, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msg.toJson() == expectedJson
	}

	void "toJson with empty content (v30)"() {
		String serializedContent = ""
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"$serializedContent\",0,null]"

		when:
		MessageID messageID = new MessageID("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId")
		MessageRef previousMessageRef = new MessageRef(1528228170000L, 0)
		StreamMessageV30 msg = new StreamMessageV30(messageID, previousMessageRef, ContentType.CONTENT_TYPE_JSON, serializedContent, SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msg.toJson() == expectedJson
	}

	void "fromJson (v28)"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessageV28 msg = (StreamMessageV28) StreamMessage.fromJson(json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getTtl() == 0
		msg.getOffset() == 1871084066
		msg.getPreviousOffset() == 1871084061
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
	}

	void "fromJson (v29)"() {
		String json = "[29,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"publisherAddress\",\"signature\"]"

		when:
		StreamMessageV29 msg = (StreamMessageV29) StreamMessage.fromJson(json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getTtl() == 0
		msg.getOffset() == 1871084066
		msg.getPreviousOffset() == 1871084061
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == SignatureType.SIGNATURE_TYPE_ETH
		msg.getPublisherId() == "publisherAddress"
		msg.getSignature() == "signature"
	}

	void "fromJson (v30)"() {
		String json = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"

		when:
		StreamMessageV30 msg = (StreamMessageV30) StreamMessage.fromJson(json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
		msg.getPreviousMessageRef().getTimestamp() == 1528228170000L
		msg.getPreviousMessageRef().getTimestampAsDate() == new Date(1528228170000L)
		msg.getPreviousMessageRef().getSequenceNumber() == 0
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson (v30) with previousMessageRef null"() {
		String json = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"

		when:
		StreamMessageV30 msg = (StreamMessageV30) StreamMessage.fromJson(json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
		msg.getPreviousMessageRef() == null
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson() with previousOffset null"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessageV28 msg = (StreamMessageV28) StreamMessage.fromJson(json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getTtl() == 0
		msg.getOffset() == 1871084066
		msg.getPreviousOffset() == null
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
	}

	void "fromJson throws for invalid version"() {
		String json = "[666,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessage.fromJson(json)

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid content type"() {
		String json = "[28,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,666,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\"]"

		when:
		StreamMessage.fromJson(json)

		then:
		thrown(UnsupportedMessageException)
	}

	void "fromJson throws for invalid message structure"() {
		String json = "[28,0]"

		when:
		StreamMessage.fromJson(json)

		then:
		thrown(MalformedMessageException)
	}
}
