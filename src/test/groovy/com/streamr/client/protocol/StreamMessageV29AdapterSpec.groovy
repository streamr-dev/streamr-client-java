package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.streamr.client.exceptions.MalformedMessageException
import com.streamr.client.exceptions.UnsupportedMessageException
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType
import com.streamr.client.protocol.message_layer.StreamMessageV28
import com.streamr.client.protocol.message_layer.StreamMessageV28Adapter
import com.streamr.client.protocol.message_layer.StreamMessageV29
import com.streamr.client.protocol.message_layer.StreamMessageV29Adapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class StreamMessageV29AdapterSpec extends Specification {
	StreamMessageV29Adapter adapter

	void setup() {
		adapter = new StreamMessageV29Adapter()
	}

	private static JsonReader toReader(String json) {
		return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
	}

	private static String msgToJson(StreamMessageV29Adapter adapter, StreamMessageV29 msg) {
		Buffer buffer = new Buffer()
		JsonWriter writer = JsonWriter.of(buffer)
		writer.beginArray()
		writer.value(msg.getVersion())
		adapter.toJson(writer, msg)
		writer.endArray()
		return buffer.readUtf8()
	}

	private static StreamMessageV29 fromJsonToMsg(StreamMessageV29Adapter adapter, String json) {
		JsonReader reader = toReader(json)
		reader.beginArray()
		reader.nextInt()
		StreamMessageV29 msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "toJson"() {
		String serializedContent = '{"desi":"2","dir":"1","oper":40,"veh":222,"tst":"2018-06-05T19:49:33Z","tsi":1528228173,"spd":3.6,"hdg":69,"lat":60.192258,"long":24.928701,"acc":-0.59,"dl":-248,"odo":5134,"drst":0,"oday":"2018-06-05","jrn":885,"line":30,"start":"22:23"}'
		String expectedJson = "[29,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"publisherAddress\",\"signature\"]"
		when:
		StreamMessageV29 msg = new StreamMessageV29("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, 1871084066, 1871084061, ContentType.CONTENT_TYPE_JSON, serializedContent, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "publisherAddress", "signature")

		then:
		msgToJson(adapter, msg) == expectedJson
	}

	void "fromJson"() {
		String json = "[29,\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,1871084066,1871084061,27,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",1,\"publisherAddress\",\"signature\"]"

		when:
		StreamMessageV29 msg = fromJsonToMsg(adapter, json)

		then:
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
		msg.getSignatureType() == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.getPublisherId() == "publisherAddress"
		msg.getSignature() == "signature"
	}
}
