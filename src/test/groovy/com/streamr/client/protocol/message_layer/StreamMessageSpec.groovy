package com.streamr.client.protocol.message_layer

import com.streamr.client.exceptions.EncryptedContentNotParsableException
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType
import com.streamr.client.testing.StreamMessageExamples
import spock.lang.Specification

class StreamMessageSpec extends Specification {
	StreamMessage msg

	void setup() {
		msg = StreamMessage.deserialize(StreamMessageExamples.InvalidSignature.helloWorldSerialized32)
	}

	void "constructor that takes Map content sets the correct serializedContent"() {
		String serializedContent = msg.getSerializedContent()
		Map<String, Object> mapContent = Json.mapAdapter.fromJson(serializedContent)

		when:
		msg = new StreamMessage.Builder(msg)
				.withSerializedContent(Json.mapAdapter.toJson(mapContent))
				.createStreamMessage()
		then:
		msg.getParsedContent() == mapContent
		msg.getSerializedContent() == serializedContent
	}

	void "getParsedContent() throws if message is AES encrypted"() {
		when:
		msg = new StreamMessage.Builder(msg)
				.withEncryptionType(EncryptionType.AES)
				.createStreamMessage()
		msg.getParsedContent()
		then:
		thrown EncryptedContentNotParsableException
	}
}
