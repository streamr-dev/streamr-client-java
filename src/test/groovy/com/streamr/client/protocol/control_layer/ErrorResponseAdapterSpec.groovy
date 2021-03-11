package com.streamr.client.protocol.control_layer

import spock.lang.Specification

class ErrorResponseAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,7,"requestId","errorMessage","ERROR_CODE"]' | new ErrorResponse("requestId", "errorMessage", "ERROR_CODE")
	}
}
