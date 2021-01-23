package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamrSpecification
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingJson
import com.streamr.client.testing.TestingMessageRef

class DecryptionQueuesSpec extends StreamrSpecification {

	DecryptionQueues decryptionQueues

	void setup() {
		decryptionQueues = new DecryptionQueues("streamId", 0)
	}

	void "messages are queued by msgChainId, and they can be drained by groupKeyIds"() {
		GroupKey key1 = GroupKey.generate()
		GroupKey key2 = GroupKey.generate()
		GroupKey pub2key = GroupKey.generate()

		final String content = TestingJson.toJson(new HashMap<String, Object>())
		// msgChain1 has messages with two different keys
		final MessageId messageId6 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(0)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain1")
				.createMessageId()
		StreamMessage chain1key1msg1 = new StreamMessage.Builder()
				.withMessageId(messageId6)
				.withSerializedContent(content)
				.createStreamMessage()
		final MessageId messageId5 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(1)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain1")
				.createMessageId()
		StreamMessage chain1key1msg2 = new StreamMessage.Builder()
				.withMessageId(messageId5)
				.withPreviousMessageRef(TestingMessageRef.createMessageRef(0, 0))
				.withSerializedContent(content)
				.createStreamMessage()
		final MessageId messageId4 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(2)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain1")
				.createMessageId()
		StreamMessage chain1key1msg3 = new StreamMessage.Builder()
				.withMessageId(messageId4)
				.withPreviousMessageRef(TestingMessageRef.createMessageRef(0, 0))
				.withSerializedContent(content)
				.createStreamMessage()
		final MessageId messageId3 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(3)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain1")
				.createMessageId()
		StreamMessage chain1key2msg4 = new StreamMessage.Builder()
				.withMessageId(messageId3)
				.withPreviousMessageRef(TestingMessageRef.createMessageRef(0, 0))
				.withSerializedContent(content)
				.createStreamMessage()

		// Also there's another msgChain from the same publisher
		final MessageId messageId2 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(0)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain2")
				.createMessageId()
		StreamMessage chain2key2msg1 = new StreamMessage.Builder()
				.withMessageId(messageId2)
				.withSerializedContent(content)
				.createStreamMessage()
		final MessageId messageId1 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(1)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChain2")
				.createMessageId()
		StreamMessage chain2key2msg2 = new StreamMessage.Builder()
				.withMessageId(messageId1)
				.withPreviousMessageRef(TestingMessageRef.createMessageRef(0, 0))
				.withSerializedContent(content)
				.createStreamMessage()

		// And a completely different publisher
		final MessageId messageId = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(0)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.createPublisherId(2))
				.withMsgChainId("pub2msgChain")
				.createMessageId()
		StreamMessage pub2msg1 = new StreamMessage.Builder()
				.withMessageId(messageId)
				.withSerializedContent(content)
				.createStreamMessage()

		// Encrypt each message with appropriate key and add to the decryptionQueues
		chain1key1msg1 = EncryptionUtil.encryptStreamMessage(chain1key1msg1, key1)
		chain1key1msg2 = EncryptionUtil.encryptStreamMessage(chain1key1msg2, key1)
		chain1key1msg3 = EncryptionUtil.encryptStreamMessage(chain1key1msg3, key1)
		decryptionQueues.add(chain1key1msg1)
		decryptionQueues.add(chain1key1msg2)
		decryptionQueues.add(chain1key1msg3)

		chain1key2msg4 = EncryptionUtil.encryptStreamMessage(chain1key2msg4, key2)
		chain2key2msg1 = EncryptionUtil.encryptStreamMessage(chain2key2msg1, key2)
		chain2key2msg2 = EncryptionUtil.encryptStreamMessage(chain2key2msg2, key2)
		decryptionQueues.add(chain1key2msg4)
		decryptionQueues.add(chain2key2msg1)
		decryptionQueues.add(chain2key2msg2)

		pub2msg1 = EncryptionUtil.encryptStreamMessage(pub2msg1, pub2key)
		decryptionQueues.add(pub2msg1)

		expect:
		!decryptionQueues.isEmpty()

		when:
		// Drain with key 1
		Collection<StreamMessage> unlockedByKey1 = decryptionQueues.drainUnlockedMessages(TestingAddresses.PUBLISHER_ID, [key1]*.groupKeyId.toSet())

		then:
		unlockedByKey1 == [chain1key1msg1, chain1key1msg2, chain1key1msg3]
		!decryptionQueues.isEmpty()

		when:
		// Drain with key 2
		Collection<StreamMessage> unlockedByKey2 = decryptionQueues.drainUnlockedMessages(TestingAddresses.PUBLISHER_ID, [key2]*.groupKeyId.toSet())

		then:
		unlockedByKey2 == [chain1key2msg4, chain2key2msg1, chain2key2msg2]
		!decryptionQueues.isEmpty()

		when:
		// Drain with publisher2's key
		Collection<StreamMessage> unlockedByPub2 = decryptionQueues.drainUnlockedMessages(TestingAddresses.createPublisherId(2), [pub2key]*.groupKeyId.toSet())

		then:
		unlockedByPub2 == [pub2msg1]
		decryptionQueues.isEmpty()
	}

}
