package com.streamr.client.protocol

import com.streamr.client.exceptions.UnsupportedPayloadException
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class PublishRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	PublishRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new PublishRequestAdapter()
		buffer = new Buffer()
	}

	void "toJson (no optional fields)"() {
		PublishRequest request = new PublishRequest(
				"streamId",
				[foo: "bar"],
				null,
				null,
				null)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"type\":\"publish\",\"stream\":\"streamId\",\"msg\":\"{\\\"foo\\\":\\\"bar\\\"}\"}"
	}

	void "toJson (with optional fields)"() {
		PublishRequest request = new PublishRequest(
				"streamId",
				[foo: "bar"],
				new Date(1535384328000),
				"foo",
				"key")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == "{\"type\":\"publish\",\"stream\":\"streamId\",\"msg\":\"{\\\"foo\\\":\\\"bar\\\"}\",\"authKey\":\"key\",\"ts\":1535384328000,\"pkey\":\"foo\"}"
	}

	void "toJson (with unsupported payload)"() {
		PublishRequest request = new PublishRequest(
				"streamId",
				new Object(), // invalid payload
				new Date(1535384328000),
				"foo",
				"key")

		when:
		adapter.toJson(buffer, request)

		then:
		thrown(UnsupportedPayloadException)
	}

}
