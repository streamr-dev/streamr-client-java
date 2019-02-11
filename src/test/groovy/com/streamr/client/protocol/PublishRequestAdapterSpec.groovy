package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ErrorResponse
import com.streamr.client.protocol.control_layer.ErrorResponseAdapter
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

	private static PublishRequest toMsg(PublishRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		PublishRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String msgJson = "[30,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\"],[1528228170000,0],27,\"{\\\"hello\\\":\\\"world\\\"}\",1,\"signature\"]"
		String json = '[1,8,'+msgJson+',"sessionToken"]'

		when:
		PublishRequest msg = toMsg(adapter, json)

		then:
		msg.streamMessage instanceof StreamMessage
		msg.sessionToken == "sessionToken"
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
