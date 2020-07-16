package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ControlMessage
import spock.lang.Specification

import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorld
import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorldSerialized31

class BroadcastMessageAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		"[2,0,\"requestId\",${helloWorldSerialized31}]" | new BroadcastMessage("requestId", helloWorld)
	}
}
