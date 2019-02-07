package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.PublishRequest
import com.streamr.client.protocol.control_layer.PublishRequestAdapter
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
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

	void "toJson"() {
		StreamMessageV30 msg = new StreamMessageV30(
				"7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", 1528228170000L, 0,
				StreamMessage.ContentType.CONTENT_TYPE_JSON, '{"hello":"world"}', StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
		PublishRequest request = new PublishRequest(msg, "sessionToken")
		String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,8,'+msgJson+',"sessionToken"]'
	}
}
