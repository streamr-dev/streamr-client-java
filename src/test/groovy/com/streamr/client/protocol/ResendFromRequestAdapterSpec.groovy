package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.ResendFromRequest
import com.streamr.client.protocol.message_layer.MessageRef
import spock.lang.Specification

class ResendFromRequestAdapterSpec extends StreamrSpecification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,12,"requestId","streamId",0,[143415425455,0],"' + publisherId + '","sessionToken"]' | new ResendFromRequest("requestId", "streamId", 0, new MessageRef(143415425455L, 0L), publisherId.toString(), "sessionToken")
		'[2,12,"requestId","streamId",0,[143415425455,0],null,null]' | new ResendFromRequest("requestId", "streamId", 0, new MessageRef(143415425455L, 0L), null)
	}

}
