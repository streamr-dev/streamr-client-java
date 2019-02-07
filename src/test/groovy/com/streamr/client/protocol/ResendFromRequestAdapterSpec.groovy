package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendFromRequest
import com.streamr.client.protocol.control_layer.ResendFromRequestAdapter
import com.streamr.client.protocol.message_layer.MessageRef
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendFromRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendFromRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendFromRequestAdapter()
		buffer = new Buffer()
	}

	private static ResendFromRequest toMsg(ResendFromRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendFromRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,12,"streamId",0,"subId",[143415425455,0],"publisherId"]'

		when:
		ResendFromRequest msg = toMsg(adapter, json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getSubId() == "subId"
		msg.getFromMsgRef().getTimestamp() == 143415425455
		msg.getFromMsgRef().getSequenceNumber() == 0
		msg.getPublisherId() == "publisherId"
	}

	void "toJson"() {
		MessageRef from = new MessageRef(143415425455L, 0L)
		ResendFromRequest request = new ResendFromRequest("streamId", 0, "subId", from, "publisherId")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,12,"streamId",0,"subId",[143415425455,0],"publisherId"]'
	}
}
