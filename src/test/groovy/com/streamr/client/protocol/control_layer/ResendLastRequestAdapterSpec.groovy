package com.streamr.client.protocol.control_layer

import spock.lang.Specification

class ResendLastRequestAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,11,"requestId","streamId",0,4,"sessionToken"]' | new ResendLastRequest("requestId","streamId", 0, 4, "sessionToken")
		'[2,11,"requestId","streamId",0,4,null]' | new ResendLastRequest("requestId","streamId", 0, 4, null)
	}

}
