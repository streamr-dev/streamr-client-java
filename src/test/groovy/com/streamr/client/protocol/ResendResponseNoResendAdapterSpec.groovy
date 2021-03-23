package com.streamr.client.protocol


import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.ResendResponseNoResend
import spock.lang.Specification

class ResendResponseNoResendAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,6,"requestId","streamId",0]' | new ResendResponseNoResend("requestId","streamId", 0)
	}

}
