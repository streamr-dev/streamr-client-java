package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.ResendResponseResending
import spock.lang.Specification

class ResendResponseResendingAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,4,"requestId","streamId",0]' | new ResendResponseResending("requestId","streamId", 0)
	}

}
