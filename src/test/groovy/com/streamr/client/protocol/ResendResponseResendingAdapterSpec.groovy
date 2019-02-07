package com.streamr.client.protocol

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

	void "toJson"() {
		ResendResponseResending request = new ResendResponseResending("streamId", 0, "subId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,4,"streamId",0,"subId"]'
	}
}
