package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType
import com.streamr.client.protocol.message_layer.StreamMessageV29
import com.streamr.client.protocol.message_layer.StreamMessageV29Adapter
import com.streamr.client.protocol.message_layer.StreamMessageV30
import com.streamr.client.protocol.message_layer.StreamMessageV30Adapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class StreamMessageV30AdapterSpec extends Specification {
	StreamMessageV30Adapter adapter

	void setup() {
		adapter = new StreamMessageV30Adapter()
	}

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

	private static String msgToJson(StreamMessageV30Adapter adapter, StreamMessageV30 msg) {
		Buffer buffer = new Buffer()
		JsonWriter writer = JsonWriter.of(buffer)
		writer.beginArray()
		writer.value(msg.getVersion())
		adapter.toJson(writer, msg)
		writer.endArray()
		return buffer.readUtf8()
	}

	private static StreamMessageV30 fromJsonToMsg(StreamMessageV30Adapter adapter, String json) {
		JsonReader reader = toReader(json)
		reader.beginArray()
		reader.nextInt()
		StreamMessageV30 msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "toJson"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"
		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson (constructor with content map)"() {
		Map content = ["desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"]
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"
		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with no signature"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with null previous message ref"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV30 msg = new StreamMessageV30("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", (Long) null, 0, ContentType.CONTENT_TYPE_JSON, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "toJson with empty content (v30)"() {
		String serializedContent = ""
		String expectedJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,\"$serializedContent\",0,null]"

		when:
		MessageID messageID = new MessageID("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId","1")
		MessageRef previousMessageRef = new MessageRef(1528228170000L, 0)
		StreamMessageV30 msg = new StreamMessageV30(messageID, previousMessageRef, ContentType.CONTENT_TYPE_JSON, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "fromJson"() {
		String json = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"

		when:
		StreamMessageV30 msg = fromJsonToMsg(adapter, json)

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
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson (v30) with previousMessageRef null"() {
		String json = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"signature\"]"

		when:
		StreamMessageV30 msg = fromJsonToMsg(adapter, json)

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
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.getSignature() == "signature"
	}

	void "fromJson (v30) with no signature"() {
		String json = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],null,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]"

		when:
		StreamMessageV30 msg = fromJsonToMsg(adapter, json)

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
		msg.getContent() instanceof Map
		msg.getContent().desi == "2"
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_NONE
	}
}
