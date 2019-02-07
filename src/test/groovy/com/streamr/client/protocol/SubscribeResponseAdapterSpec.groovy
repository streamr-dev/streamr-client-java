package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.SubscribeResponse
import com.streamr.client.protocol.control_layer.SubscribeResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class SubscribeResponseAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	SubscribeResponseAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new SubscribeResponseAdapter()
		buffer = new Buffer()
	}

	void "toJson"() {
		SubscribeResponse request = new SubscribeResponse("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,2,"streamId",0]'
	}
}
