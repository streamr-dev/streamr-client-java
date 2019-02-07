package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.SubscribeRequest
import com.streamr.client.protocol.control_layer.SubscribeRequestAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class SubscribeRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	SubscribeRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new SubscribeRequestAdapter()
		buffer = new Buffer()
	}

	void "toJson"() {
		SubscribeRequest request = new SubscribeRequest("streamId", 0, "sessionToken")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,9,"streamId",0,"sessionToken"]'
	}
}
