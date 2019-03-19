package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.UnsubscribeRequest
import com.streamr.client.protocol.control_layer.UnsubscribeRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class UnsubscribeRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	UnsubscribeRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new UnsubscribeRequestAdapter()
		buffer = new Buffer()
	}

	private static UnsubscribeRequest toMsg(UnsubscribeRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		UnsubscribeRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,10,"streamId",0]'

		when:
		UnsubscribeRequest msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
	}

	void "toJson"() {
		UnsubscribeRequest request = new UnsubscribeRequest("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,10,"streamId",0]'
	}
}
