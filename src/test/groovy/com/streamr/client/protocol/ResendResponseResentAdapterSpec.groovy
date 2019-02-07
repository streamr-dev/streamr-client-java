package com.streamr.client.protocol

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

	void "toJson"() {
		ResendResponseResent request = new ResendResponseResent("streamId", 0, "subId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,5,"streamId",0,"subId"]'
	}
}
