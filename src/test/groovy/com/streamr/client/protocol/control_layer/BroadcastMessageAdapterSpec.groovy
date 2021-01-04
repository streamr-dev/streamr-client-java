package com.streamr.client.protocol.control_layer

import spock.lang.Specification

import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorld
import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorldSerialized32

class BroadcastMessageAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		"[2,0,\"requestId\",${helloWorldSerialized32}]" | new BroadcastMessage("requestId", helloWorld)
	}
}
