package com.streamr.client.protocol.control_layer

import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorld
import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorldSerialized32

import spock.lang.Specification

class UnicastMessageAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage                               | message
		"[2,1,\"requestId\",${helloWorldSerialized32}]" | new UnicastMessage("requestId", helloWorld)
	}

}
