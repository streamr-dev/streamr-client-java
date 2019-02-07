package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.BroadcastMessageAdapter
import com.streamr.client.protocol.control_layer.PublishRequest
import com.streamr.client.protocol.control_layer.PublishRequestAdapter
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class BroadcastMessageAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	BroadcastMessageAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new BroadcastMessageAdapter()
		buffer = new Buffer()
	}

	void "toJson"() {
		StreamMessageV30 msg = new StreamMessageV30(
				"7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", 1528228170000L, 0,
				StreamMessage.ContentType.CONTENT_TYPE_JSON, '{"hello":"world"}', StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
		BroadcastMessage broadcastMessage = new BroadcastMessage(msg)
		String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"

		when:
		adapter.toJson(buffer, broadcastMessage)

		then:
		buffer.readString(utf8) == '[1,0,'+msgJson+']'
	}
}
