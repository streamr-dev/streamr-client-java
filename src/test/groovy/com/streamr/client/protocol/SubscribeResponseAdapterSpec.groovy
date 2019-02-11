package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.SubscribeResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class SubscribeResponseAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	SubscribeResponseAdapter adapter
	Buffer buffer

	private static SubscribeResponse toMsg(SubscribeResponseAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		SubscribeResponse msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void setup() {
		adapter = new SubscribeResponseAdapter()
		buffer = new Buffer()
	}

	void "fromJson"() {
		String json = '[1,2,"streamId",0]'

		when:
		SubscribeResponse msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
	}

	void "toJson"() {
		SubscribeResponse request = new SubscribeResponse("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,2,"streamId",0]'
	}
}
