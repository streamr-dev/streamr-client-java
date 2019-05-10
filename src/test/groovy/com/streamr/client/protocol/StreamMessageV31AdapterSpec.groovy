package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.streamr.client.exceptions.ContentTypeNotParsableException
import com.streamr.client.exceptions.EncryptedContentNotParsableException
import com.streamr.client.protocol.message_layer.*
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class StreamMessageV31AdapterSpec extends Specification {
	StreamMessageV31Adapter adapter

	void setup() {
		adapter = new StreamMessageV31Adapter()
	}

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

	private static String msgToJson(StreamMessageV31Adapter adapter, StreamMessageV31 msg) {
		Buffer buffer = new Buffer()
		JsonWriter writer = JsonWriter.of(buffer)
		writer.beginArray()
		writer.value(msg.getVersion())
		adapter.toJson(writer, msg)
		writer.endArray()
		return buffer.readUtf8()
	}

	private static StreamMessageV31 fromJsonToMsg(StreamMessageV31Adapter adapter, String json) {
		JsonReader reader = toReader(json)
		reader.beginArray()
		reader.nextInt()
		StreamMessageV31 msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "toJson"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"
		when:
		StreamMessageV31 msg = new StreamMessageV31("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.NONE, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson (constructor with content map)"() {
		Map content = ["desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"]
		String expectedJson = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,1,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"
		when:
		StreamMessageV31 msg = new StreamMessageV31("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with no signature"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,2,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV31 msg = new StreamMessageV31("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.AES, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with null previous message ref"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,3,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV31 msg = new StreamMessageV31("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", (Long) null, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.NEW_KEY_AND_AES, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with empty content"() {
		String serializedContent = ""
		String expectedJson = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,0,\"$serializedContent\",0,null]"

		when:
		MessageID messageID = new MessageID("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId","1")
		MessageRef previousMessageRef = new MessageRef(1528228170000L, 0)
		StreamMessageV31 msg = new StreamMessageV31(messageID, previousMessageRef, ContentType.CONTENT_TYPE_JSON, EncryptionType.NONE, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "fromJson"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"

		when:
		StreamMessageV31 msg = fromJsonToMsg(adapter, json)

		then:
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef().getTimestamp() == 1528228170000L
		msg.getPreviousMessageRef().getTimestampAsDate() == new Date(1528228170000L)
		msg.getPreviousMessageRef().getSequenceNumber() == 0
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getEncryptionType() == EncryptionType.NONE
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson with previousMessageRef null"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]"

		when:
		StreamMessageV31 msg = fromJsonToMsg(adapter, json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getEncryptionType() == EncryptionType.NONE
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson with no signature"() {
		String json = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV31 msg = fromJsonToMsg(adapter, json)

		then:
		msg.toJson() == json
		msg.getStreamId() == "7wa7APtlTq6EC5iTCBy6dw"
		msg.getStreamPartition() == 0
		msg.getTimestamp() == 1528228173462L
		msg.getTimestampAsDate() == new Date(1528228173462L)
		msg.getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
		msg.getMsgChainId() == "1"
		msg.getPreviousMessageRef() == null
		msg.getContentType() == ContentType.CONTENT_TYPE_JSON
		msg.getEncryptionType() == EncryptionType.NONE
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_NONE
	}

	void "fromJson() with content types other than JSON"() {
		String json1 = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,28,2,\"some group key request\",0,null]"
		String json2 = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,29,2,\"some group key\",0,null]"
		String json3 = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,30,2,\"some new group key\",0,null]"

		when:
		StreamMessageV31 msg1 = fromJsonToMsg(adapter, json1)
		msg1.getContent()
		then:
		thrown ContentTypeNotParsableException
		msg1.toJson() == json1
		msg1.getContentType() == ContentType.GROUP_KEY_REQUEST
		msg1.getSerializedContent() == "some group key request"
		when:
		StreamMessageV31 msg2 = fromJsonToMsg(adapter, json2)
		msg2.getContent()
		then:
		thrown ContentTypeNotParsableException
		msg2.toJson() == json2
		msg2.getContentType() == ContentType.GROUP_KEY_RESPONSE_SIMPLE
		msg2.getSerializedContent() == "some group key"
		when:
		StreamMessageV31 msg3 = fromJsonToMsg(adapter, json3)
		msg3.getContent()
		then:
		thrown ContentTypeNotParsableException
		msg3.toJson() == json3
		msg3.getContentType() == ContentType.GROUP_KEY_RESET_SIMPLE
		msg3.getSerializedContent() == "some new group key"
	}

	void "getContent() throws if encrypted"() {
		StreamMessageV31 msg1 = new StreamMessageV31("", 0, 0L, 0, "", "", 0L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.RSA, "encrypted content", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
		StreamMessageV31 msg2 = new StreamMessageV31("", 0, 0L, 0, "", "", 0L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.AES, "encrypted content", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
		StreamMessageV31 msg3 = new StreamMessageV31("", 0, 0L, 0, "", "", 0L, 0, ContentType.CONTENT_TYPE_JSON, EncryptionType.NEW_KEY_AND_AES, "encrypted content", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		when:
		msg1.getContent()
		then:
		thrown EncryptedContentNotParsableException
		when:
		msg2.getContent()
		then:
		thrown EncryptedContentNotParsableException
		when:
		msg3.getContent()
		then:
		thrown EncryptedContentNotParsableException
	}
}
