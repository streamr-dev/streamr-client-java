package com.streamr.client.protocol.control_layer

import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageRef

class ResendRangeRequestAdapterSpec extends StreamrSpecification {

	def "serialization and deserialization"(String serializedMessage, ControlMessage message) {
		expect:
		ControlMessage.fromJson(serializedMessage) == message
		message.toJson() == serializedMessage

		where:
		serializedMessage | message
		'[2,13,"requestId","streamId",0,[143415425455,0],[14341542564555,7],"publisherid","msgChainId","sessionToken"]' | new ResendRangeRequest("requestId", "streamId", 0, new MessageRef(143415425455L, 0L), new MessageRef(14341542564555L, 7L), publisherId, "msgChainId", "sessionToken")
		'[2,13,"requestId","streamId",0,[143415425455,0],[14341542564555,7],null,null,null]' | new ResendRangeRequest("requestId", "streamId", 0, new MessageRef(143415425455L, 0L), new MessageRef(14341542564555L, 7L), null)
	}

	void "fromJson (from > to)"() {
		String serializedMessage = '[2,13,"requestId","streamId",0,[143415425455,0],[143415425000,0],"publisherId","msgChainId","sessionToken"]'

		when:
		ControlMessage.fromJson(serializedMessage)

		then:
		thrown(IllegalArgumentException)
	}

}
