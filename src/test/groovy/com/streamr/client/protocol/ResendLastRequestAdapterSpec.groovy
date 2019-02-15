package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendLastRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendLastRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendLastRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendLastRequestAdapter()
		buffer = new Buffer()
	}

	private static ResendLastRequest toMsg(ResendLastRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendLastRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,11,"streamId",0,"subId",4,"sessionToken"]'

		when:
		ResendLastRequest msg = toMsg(adapter, json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getSubId() == "subId"
		msg.getNumberLast() == 4
		msg.getSessionToken() == "sessionToken"
	}

	void "toJson"() {
		ResendLastRequest request = new ResendLastRequest("streamId", 0, "subId", 4, "sessionToken")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,11,"streamId",0,"subId",4,"sessionToken"]'
	}
}
