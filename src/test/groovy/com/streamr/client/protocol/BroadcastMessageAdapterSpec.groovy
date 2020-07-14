package com.streamr.client.protocol

import com.streamr.client.protocol.control_layer.BroadcastMessage
import com.streamr.client.protocol.control_layer.ControlMessage
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import spock.lang.Specification

class BroadcastMessageAdapterSpec extends Specification {

	static final StreamMessageV31 streamMessage = new StreamMessageV31(
			"7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, "publisherId", "1", 1528228170000L, 0,
			StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, '{"hello":"world"}', StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
	static final String serializedStreamMessage = "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherId\",\"1\"],[1528228170000,0],27,0,\"{\\\"hello\\\":\\\"world\\\"}\",2,\"signature\"]"

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		"[2,0,\"requestId\",${serializedStreamMessage}]" | new BroadcastMessage("requestId", streamMessage)
	}
}
