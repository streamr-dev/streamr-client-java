package com.streamr.client.protocol.control_layer

import spock.lang.Specification

class ResendResponseResentAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,5,"requestId","streamId",0]' | new ResendResponseResent("requestId", "streamId", 0)
	}

}
