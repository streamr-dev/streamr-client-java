package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendResponseNoResend
import com.streamr.client.protocol.control_layer.ResendResponseNoResendAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendResponseNoResendAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendResponseNoResendAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendResponseNoResendAdapter()
		buffer = new Buffer()
	}

	private static ResendResponseNoResend toMsg(ResendResponseNoResendAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendResponseNoResend msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,6,"streamId",0,"subId"]'

		when:
		ResendResponseNoResend msg = toMsg(adapter, json)

		then:
		msg.streamId == "streamId"
		msg.streamPartition == 0
		msg.subId == "subId"
	}

	void "toJson"() {
		ResendResponseNoResend request = new ResendResponseNoResend("streamId", 0, "subId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,6,"streamId",0,"subId"]'
	}
}
