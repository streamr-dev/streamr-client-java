package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.SubscribeRequest
import spock.lang.Specification

class SubscribeRequestAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,9,"requestId","streamId",0,"sessionToken"]' | new SubscribeRequest("requestId", "streamId", 0, "sessionToken")
		'[2,9,"requestId","streamId",0,null]' | new SubscribeRequest("requestId", "streamId", 0, null)
	}

}
