package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendResponseResending
import com.streamr.client.protocol.control_layer.ResendResponseResendingAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendResponseResendingAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendResponseResendingAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendResponseResendingAdapter()
		buffer = new Buffer()
	}

	private static ResendResponseResending toMsg(ResendResponseResendingAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendResponseResending msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,4,"streamId",0,"subId"]'

		when:
		ResendResponseResending msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
		msg.subId == "subId"
	}

	void "toJson"() {
		ResendResponseResending request = new ResendResponseResending("streamId", 0, "subId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,4,"streamId",0,"subId"]'
	}
}
