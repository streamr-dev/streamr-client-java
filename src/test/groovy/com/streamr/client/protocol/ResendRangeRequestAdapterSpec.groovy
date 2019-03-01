package com.streamr.client.protocol

import com.squareup.moshi.JsonReader
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.control_layer.ResendRangeRequestAdapter
import com.streamr.client.protocol.message_layer.MessageRef
import okio.Buffer
import spock.lang.Specification

import java.nio.charset.Charset

class ResendRangeRequestAdapterSpec extends Specification {

	private static Charset utf8 = Charset.forName("UTF-8")

	ResendRangeRequestAdapter adapter
	Buffer buffer

	void setup() {
		adapter = new ResendRangeRequestAdapter()
		buffer = new Buffer()
	}

	private static ResendRangeRequest toMsg(ResendRangeRequestAdapter adapter, String json) {
		JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
		reader.beginArray()
		reader.nextInt()
		reader.nextInt()
		ResendRangeRequest msg = adapter.fromJson(reader)
		reader.endArray()
		return msg
	}

	void "fromJson"() {
		String json = '[1,13,"streamId",0,"subId",[143415425455,0],[14341542564555,7],"publisherId","msgChainId","sessionToken"]'

		when:
		ResendRangeRequest msg = toMsg(adapter, json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getSubId() == "subId"
		msg.getFromMsgRef().getTimestamp() == 143415425455
		msg.getFromMsgRef().getSequenceNumber() == 0
		msg.getToMsgRef().getTimestamp() == 14341542564555
		msg.getToMsgRef().getSequenceNumber() == 7
		msg.getPublisherId() == "publisherId"
		msg.getMsgChainId() == "msgChainId"
		msg.getSessionToken() == "sessionToken"
	}

	void "fromJson (null fields)"() {
		String json = '[1,13,"streamId",0,"subId",[143415425455,0],[14341542564555,7],null,null,null]'

		when:
		ResendRangeRequest msg = toMsg(adapter, json)

		then:
		msg.getStreamId() == "streamId"
		msg.getStreamPartition() == 0
		msg.getSubId() == "subId"
		msg.getFromMsgRef().getTimestamp() == 143415425455
		msg.getFromMsgRef().getSequenceNumber() == 0
		msg.getToMsgRef().getTimestamp() == 14341542564555
		msg.getToMsgRef().getSequenceNumber() == 7
		msg.getPublisherId() == null
		msg.getMsgChainId() == null
		msg.getSessionToken() == null
	}

	void "toJson"() {
		MessageRef from = new MessageRef(143415425455L, 0L)
		MessageRef to = new MessageRef(14341542564555L, 7L)
		ResendRangeRequest request = new ResendRangeRequest("streamId", 0, "subId", from, to, "publisherId", "msgChainId", "sessionToken")

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,13,"streamId",0,"subId",[143415425455,0],[14341542564555,7],"publisherId","msgChainId","sessionToken"]'
	}

	void "toJson (null fields)"() {
		MessageRef from = new MessageRef(143415425455L, 0L)
		MessageRef to = new MessageRef(14341542564555L, 7L)
		ResendRangeRequest request = new ResendRangeRequest("streamId", 0, "subId", from, to, null, null, null)

		when:
		adapter.toJson(buffer, request)

		then:
		buffer.readString(utf8) == '[1,13,"streamId",0,"subId",[143415425455,0],[14341542564555,7],null,null,null]'
	}
}
