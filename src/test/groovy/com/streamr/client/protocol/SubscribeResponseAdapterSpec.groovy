package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.SubscribeResponse
import spock.lang.Specification

class SubscribeResponseAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,2,"requestId","streamId",0]' | new SubscribeResponse("requestId", "streamId", 0)
	}

}
