package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.UnsubscribeResponse
import com.streamr.client.protocol.control_layer.UnsubscribeResponseAdapter
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class UnsubscribeResponseAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	UnsubscribeResponseAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new UnsubscribeResponseAdapter()
		buffer = new Buffer()
	}

	void "toJson"() {
		UnsubscribeResponse request = new UnsubscribeResponse("streamId", 0)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,3,"streamId",0]'
	}
}
