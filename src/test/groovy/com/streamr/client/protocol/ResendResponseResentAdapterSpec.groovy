package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendResponseResent
import com.streamr.client.protocol.control_layer.ResendResponseResentAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendResponseResentAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendResponseResentAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendResponseResentAdapter()
		buffer = new Buffer()
	}

	private static ResendResponseResent toMsg(ResendResponseResentAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendResponseResent msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,5,"streamId",0,"requestId"]'

		when:
		ResendResponseResent msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
		msg.requestId == "requestId"
	}

	void "toJson"() {
		ResendResponseResent request = new ResendResponseResent("streamId", 0, "requestId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,5,"streamId",0,"requestId"]'
	}
}
