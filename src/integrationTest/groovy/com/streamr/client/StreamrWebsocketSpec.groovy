package com.streamr.client

import com.streamr.client.options.ResendFromOption
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendRangeOption
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.UnencryptedGroupKey
import org.apache.commons.codec.binary.Hex
import org.java_websocket.WebSocket
import org.java_websocket.enums.ReadyState
import spock.util.concurrent.PollingConditions

import java.security.SecureRandom

class StreamrWebsocketSpec extends StreamrIntegrationSpecification {

	private StreamrClient client
	private SecureRandom secureRandom = new SecureRandom()

	UnencryptedGroupKey genKey() {
		byte[] keyBytes = new byte[32]
		secureRandom.nextBytes(keyBytes)
		return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes), new Date())
	}

	void setup() {
		client = createClientWithPrivateKey(generatePrivateKey())
	}

	void cleanup() {
		if (client != null && client.state != ReadyState.CLOSED) {
			client.disconnect()
		}
	}

	void "client can connect and disconnect over websocket"() {
		when:
		client.connect()

		then:
		client.state == ReadyState.OPEN

		when:
		client.disconnect()

		then:
		client.state == ReadyState.CLOSED
	}


	void "client automatically connects for publishing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.publish(stream, [foo: "bar"], new Date())

		then:
		client.state == ReadyState.OPEN
	}

	void "client automatically connects for subscribing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {}
		})

		then:
		client.state == ReadyState.OPEN
	}

	void "a subscriber receives published messages"() {
		int msgCount = 0
		int timeout = 10 * 1000
		Subscription sub

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		// Subscribe to the stream
		StreamMessage latestMsg
		sub = client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				msgCount++
				latestMsg = message
			}

		})

		Thread.sleep(2000)

		// Produce messages to the stream
		for (int i=1; i<=10; i++) {
			client.publish(stream, [i: i])
			Thread.sleep(200)
		}

		// Allow some time for the messages to be received
		while (msgCount < 10 && timeout > 0) {
			Thread.sleep(200)
			timeout -= 200
		}

		then:
		// All messages have been received by subscriber
		msgCount == 10
		timeout > 0
		latestMsg.content.i == 10

		when:
		client.unsubscribe(sub)
		Thread.sleep(2000)
		client.publish(stream, [i: 11])

		then:
		// No more messages should be received, since we're unsubscribed
		msgCount == 10
	}

	void "subscriber receives signed message if published with signature"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		// Subscribe to the stream
		StreamMessageV31 msg
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification didn't throw
				msg = (StreamMessageV31) message
			}
		})

		Thread.sleep(2000)

		client.publish(stream, [test: 'signed'])

		Thread.sleep(2000)

		then:
		msg.getPublisherId() == client.getPublisherId()
		msg.signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.signature != null
	}

	void "subscriber can decrypt messages when he knows the keys used to encrypt"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))
		UnencryptedGroupKey key = genKey()
		HashMap<String, UnencryptedGroupKey> keys = new HashMap<>()
		keys.put(client.getPublisherId(), key)

		when:
		// Subscribe to the stream
		StreamMessageV31 msg
		client.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification and decryption didn't throw
				msg = (StreamMessageV31) message
			}
		}, null, keys)

		Thread.sleep(2000)

		client.publish(stream, [test: 'clear text'], new Date(), null, key)

		Thread.sleep(2000)

		then:
		msg.getContent() == [test: 'clear text']

		when:
		// publishing a second message with a new group key
		client.publish(stream, [test: 'another clear text'], new Date(), null, genKey())

		Thread.sleep(2000)

		then:
		// no need to explicitly give the new group key to the subscriber
		msg.getContent() == [test: 'another clear text']
	}

	void "subscriber can get the group key and decrypt encrypted messages using an RSA key pair"() {
		given:
		PollingConditions conditions1 = new PollingConditions(timeout: 10)
		PollingConditions conditions2 = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))
		UnencryptedGroupKey key = genKey()

		when:
		// Subscribe to the stream without knowing the group key
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification and decryption didn't throw
				if (msg1 == null) {
					msg1 = message
				} else {
					msg2 = message
				}
			}
		})

		Thread.sleep(2000)

		client.publish(stream, [test: 'clear text'], new Date(), null, key)

		then:
		conditions1.eventually {
			assert msg1 != null
			// the subscriber got the group key and can decrypt
			assert msg1.getContent() == [test: 'clear text']
		}

		when:
		// publishing a second message with a new group key
		client.publish(stream, [test: 'another clear text'], new Date(), null, genKey())

		then:
		conditions2.eventually {
			assert msg2 != null
			// no need to explicitly give the new group key to the subscriber
			msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscriber can get the new group key after reset and decrypt encrypted messages"() {
		given:
		PollingConditions conditions1 = new PollingConditions(timeout: 10)
		PollingConditions conditions2 = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))
		UnencryptedGroupKey key = genKey()

		when:
		// Subscribe to the stream without knowing the group key
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification and decryption didn't throw
				if (msg1 == null) {
					msg1 = message
				} else {
					msg2 = message
				}
			}
		})

		Thread.sleep(2000)

		client.publish(stream, [test: 'clear text'], new Date(), null, key)

		then:
		conditions1.eventually {
			assert msg1 != null
			// the subscriber got the group key and can decrypt
			assert msg1.getContent() == [test: 'clear text']
		}

		when:
		// publishing a second message after a rekey to revoke old subscribers
		client.rekey(stream)
		client.publish(stream, [test: 'another clear text'], new Date(), null)

		then:
		conditions2.eventually {
			assert msg2 != null
			// no need to explicitly give the new group key to the subscriber
			msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscriber can get the historical keys and decrypt old encrypted messages using an RSA key pair"() {
		given:
		PollingConditions conditions = new PollingConditions(timeout: 10)
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))
		// publishing historical messages with different group keys before subscribing
		client.publish(stream, [test: 'clear text'], new Date(), null, genKey())
		client.publish(stream, [test: 'another clear text'], new Date(), null, genKey())
		Thread.sleep(3000)

		when:
		// Subscribe to the stream with resend last without knowing the group keys
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		client.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification and decryption didn't throw
				if (msg1 == null) {
					msg1 = message
				} else if (msg2 == null) {
					msg2 = message
				} else {
					throw new RuntimeException("Received unexpected message: " + message.toJson())
				}

			}
		}, new ResendLastOption(2))

		then:
		conditions.eventually {
			assert msg1 != null && msg2 != null
			// the subscriber got the group keys and can decrypt the old messages
			assert msg1.getContent() == [test: 'clear text']
			assert msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscribe with resend last"() {
		Subscription sub

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		boolean received = false

		when:
		client.publish(stream, [i: 1])
		Thread.sleep(6000) // wait to land in storage
		// Subscribe to the stream
		sub = client.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				received = message.getContent() == [i: 1]
			}
		}, new ResendLastOption(1))

		Thread.sleep(6000)

		then:
		received
		client.unsubscribe(sub)
	}

	void "subscribe with resend from"() {
        given:
        def conditions = new PollingConditions(timeout: 10)

        Stream stream = client.createStream(new Stream(generateResourceName(), ""))

        boolean received = false
        boolean done = false

        when:
        client.publish(stream, [i: 1])
		Thread.sleep(2000)

        // Subscribe to the stream
        Subscription sub = client.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                received = message.getContent() == [i: 1]
            }
			void done(Subscription sub) {
                done = true
            }
        }, new ResendFromOption(new Date(0)))

        then:
        conditions.eventually() {
            assert done
            assert received
            client.unsubscribe(sub)
        }
	}

	void "resend last"() {
		given:
		def conditions = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		List receivedMsg = []
		boolean done = false

		when:
		for (int i = 0; i <= 10; i++) {
			client.publish(stream, [i: i])
		}
		Thread.sleep(6000) // wait to land in storage

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg.push(message.getContent())
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendLastOption(5))

		then:
		List expectedMessages = [
			Collections.singletonMap("i", 6.0),
			Collections.singletonMap("i", 7.0),
			Collections.singletonMap("i", 8.0),
			Collections.singletonMap("i", 9.0),
			Collections.singletonMap("i", 10.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}

	void "resend from"() {
		given:
		def conditions = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		List receivedMsg = []
		boolean done = false
		Date resendFromDate

		when:
		for (int i = 0; i <= 10; i++) {
			client.publish(stream, [i: i])

			if (i == 7) {
				resendFromDate = new Date()
			}
		}
		Thread.sleep(6000) // wait to land in storage

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg.push(message.getContent())
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendFromOption(resendFromDate))

		then:
		List expectedMessages = [
				Collections.singletonMap("i", 8.0),
				Collections.singletonMap("i", 9.0),
				Collections.singletonMap("i", 10.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}

	void "resend range"() {
		given:
		def conditions = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		List receivedMsg = []
		boolean done = false
		Date resendFromDate
		Date resendToDate

		when:
		for (int i = 0; i <= 10; i++) {
			Date date = new Date()
			client.publish(stream, [i: i], date)

			if (i == 3) {
				resendFromDate = new Date(date.getTime() + 1)
			}

			if (i == 7) {
				resendToDate = new Date(date.getTime() - 1)
			}
		}
		Thread.sleep(6000) // wait to land in storage

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg.push(message.getContent())
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendRangeOption(resendFromDate, resendToDate))

		then:
		List expectedMessages = [
				Collections.singletonMap("i", 4.0),
				Collections.singletonMap("i", 5.0),
				Collections.singletonMap("i", 6.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}
}
