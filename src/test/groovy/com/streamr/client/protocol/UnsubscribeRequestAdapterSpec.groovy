package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.UnsubscribeRequest
import com.streamr.client.protocol.control_layer.UnsubscribeRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class UnsubscribeRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	UnsubscribeRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new UnsubscribeRequestAdapter()
		buffer = new Buffer()
	}

	void "toJson"() {
		UnsubscribeRequest request = new UnsubscribeRequest("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,10,"streamId",0]'
	}
}
