package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.UnsubscribeRequest
import spock.lang.Specification

class UnsubscribeRequestAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,10,"requestId","streamId",0]' | new UnsubscribeRequest("requestId", "streamId", 0)
	}

}
