package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.PublishRequest
import spock.lang.Specification

import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorld
import static com.streamr.client.protocol.StreamMessageExamples.InvalidSignature.helloWorldSerialized32

class PublishRequestAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		"[2,8,\"requestId\",${helloWorldSerialized32},\"sessionToken\"]" | new PublishRequest("requestId", helloWorld, "sessionToken")
		"[2,8,\"requestId\",${helloWorldSerialized32},null]" | new PublishRequest("requestId", helloWorld, null)
	}

}
