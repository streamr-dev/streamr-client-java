package com.streamr.client.utils

import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.StreamMessage

class DecryptionQueuesSpec extends StreamrSpecification {

	DecryptionQueues decryptionQueues

	void setup() {
		decryptionQueues = new DecryptionQueues("streamId", 0)
	}

	void "messages are queued by msgChainId, and they can be drained by groupKeyIds"() {
		GroupKey key1 = GroupKey.generate()
		GroupKey key2 = GroupKey.generate()
		GroupKey pub2key = GroupKey.generate()

		// msgChain1 has messages with two different keys
		StreamMessage chain1key1msg1 = createMessage(0, 0, null, null, publisherId, [:], "msgChain1")
		StreamMessage chain1key1msg2 = createMessage(1, 0, 0, 0, publisherId, [:], "msgChain1")
		StreamMessage chain1key1msg3 = createMessage(2, 0, 0, 0, publisherId, [:], "msgChain1")
		StreamMessage chain1key2msg4 = createMessage(3, 0, 0, 0, publisherId, [:], "msgChain1")

		// Also there's another msgChain from the same publisher
		StreamMessage chain2key2msg1 = createMessage(0, 0, null, null, publisherId, [:], "msgChain2")
		StreamMessage chain2key2msg2 = createMessage(1, 0, 0, 0, publisherId, [:], "msgChain2")

		// And a completely different publisher
		StreamMessage pub2msg1 = createMessage(0, 0, null, null, getPublisherId(2), [:], "pub2msgChain")

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
		Collection<StreamMessage> unlockedByKey1 = decryptionQueues.drainUnlockedMessages(publisherId, [key1]*.groupKeyId.toSet())

		then:
		unlockedByKey1 == [chain1key1msg1, chain1key1msg2, chain1key1msg3]
		!decryptionQueues.isEmpty()

		when:
		// Drain with key 2
		Collection<StreamMessage> unlockedByKey2 = decryptionQueues.drainUnlockedMessages(publisherId, [key2]*.groupKeyId.toSet())

		then:
		unlockedByKey2 == [chain1key2msg4, chain2key2msg1, chain2key2msg2]
		!decryptionQueues.isEmpty()

		when:
		// Drain with publisher2's key
		Collection<StreamMessage> unlockedByPub2 = decryptionQueues.drainUnlockedMessages(getPublisherId(2), [pub2key]*.groupKeyId.toSet())

		then:
		unlockedByPub2 == [pub2msg1]
		decryptionQueues.isEmpty()
	}

}
