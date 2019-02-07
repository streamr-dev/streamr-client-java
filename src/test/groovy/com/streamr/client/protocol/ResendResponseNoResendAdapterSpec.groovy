package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.ResendLastRequest
import com.streamr.client.protocol.control_layer.ResendLastRequestAdapter
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

	void "toJson"() {
		ResendResponseNoResend request = new ResendResponseNoResend("streamId", 0, "subId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,6,"streamId",0,"subId"]'
	}
}
