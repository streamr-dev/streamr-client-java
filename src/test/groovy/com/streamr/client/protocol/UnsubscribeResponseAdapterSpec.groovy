package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.UnsubscribeResponse
import com.streamr.client.protocol.control_layer.UnsubscribeResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class UnsubscribeResponseAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	UnsubscribeResponseAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new UnsubscribeResponseAdapter()
		buffer = new Buffer()
	}

	private static UnsubscribeResponse toMsg(UnsubscribeResponseAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		UnsubscribeResponse msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,3,"streamId",0]'

		when:
		UnsubscribeResponse msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
	}

	void "toJson"() {
		UnsubscribeResponse request = new UnsubscribeResponse("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,3,"streamId",0]'
	}
}
