package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class SubscribeRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	SubscribeRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new SubscribeRequestAdapter()
		buffer = new Buffer()
	}

	private static SubscribeRequest toMsg(SubscribeRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		SubscribeRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,9,"streamId",0,"sessionToken"]'

		when:
		SubscribeRequest msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
		msg.sessionToken == "sessionToken"
	}

	void "fromJson (null session token)"() {
		String json = '[1,9,"streamId",0,null]'

		when:
		SubscribeRequest msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
		msg.sessionToken == null
	}

	void "toJson"() {
		SubscribeRequest request = new SubscribeRequest("streamId", 0, "sessionToken")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,9,"streamId",0,"sessionToken"]'
	}
}
