package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.control_layer.ErrorResponse
import spock.lang.Specification

class ErrorResponseAdapterSpec extends Specification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,7,"requestId","errorMessage","ERROR_CODE"]' | new ErrorResponse("requestId", "error", "ERROR_CODE")
	}
}
