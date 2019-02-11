package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.UnicastMessage
import com.streamr.client.protocol.control_layer.UnicastMessageAdapter
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class UnicastMessageAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	UnicastMessageAdapter adapter
	Buffer buffer

	private static UnicastMessage toMsg(UnicastMessageAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		UnicastMessage msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void setup() {
		adapter = new UnicastMessageAdapter()
		buffer = new Buffer()
	}

	void "fromJson"() {
		String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"
		String json = '[1,1,"subId",'+msgJson+']'

		when:
		UnicastMessage msg = toMsg(adapter, json)

		then:
		msg.getSubId() == "subId"
		msg.getStreamMessage() instanceof StreamMessage
	}

	void "toJson"() {
		StreamMessageV30 msg = new StreamMessageV30(
				"7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", 1528228170000L, 0,
				StreamMessage.ContentType.CONTENT_TYPE_JSON, '{"hello":"world"}', StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
		UnicastMessage unicastMessage = new UnicastMessage("subId", msg)
		String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"

		when:
		adapter.toJson(buffer, unicastMessage)

		then:
		buffer.readString(utf8) == '[1,1,"subId",'+msgJson+']'
	}
}
