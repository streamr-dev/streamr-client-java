package com.streamr.client.protocol

import com.streamr.client.exceptions.EncryptedContentNotParsableException
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType
import com.streamr.client.utils.HttpUtils
import spock.lang.Specification

class StreamMessageSpec extends Specification {

	StreamMessage msg

	void setup() {
		msg = StreamMessage.deserialize(StreamMessageExamples.InvalidSignature.helloWorldSerialized32)
	}

	void "constructor that takes Map content sets the correct serializedContent"() {
		String serializedContent = msg.getSerializedContent()
		Map<String, Object> mapContent = HttpUtils.mapAdapter.fromJson(serializedContent)

		when:
		msg = new StreamMessage(
				msg.getMessageID(),
				msg.getMessageRef(),
				mapContent)
		then:
		msg.getParsedContent() == mapContent
		msg.getSerializedContent() == serializedContent

	}

	void "getParsedContent() throws if message is AES encrypted"() {
		when:
		msg.setEncryptionType(EncryptionType.AES)
		msg.getParsedContent()
		then:
		thrown EncryptedContentNotParsableException
	}
}
