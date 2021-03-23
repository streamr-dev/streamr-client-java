package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.UnsubscribeResponse
import spock.lang.Specification

class UnsubscribeResponseAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,3,"requestId","streamId",0]' | new UnsubscribeResponse("requestId", "streamId", 0)
	}

}
