package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.UnicastMessage
import spock.lang.Specification

import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.*

class UnicastMessageAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage                               | message
		"[2,1,\"requestId\",${helloWorldSerialized31}]" | new UnicastMessage("requestId", helloWorld)
	}

}
