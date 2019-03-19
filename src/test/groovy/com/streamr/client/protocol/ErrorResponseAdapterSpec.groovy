package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ErrorResponse
import com.streamr.client.protocol.control_layer.ErrorResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ErrorResponseAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ErrorResponseAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ErrorResponseAdapter()
		buffer = new Buffer()
	}

	private static ErrorResponse toMsg(ErrorResponseAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ErrorResponse msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,7,"error"]'

		when:
		ErrorResponse msg = toMsg(adapter, json)

		then:
		msg.errorMessage == "error"
	}

	void "toJson"() {
		ErrorResponse request = new ErrorResponse("error")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,7,"error"]'
	}
}
