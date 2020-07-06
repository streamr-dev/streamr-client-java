package com.streamr.client

import com.streamr.client.options.ResendFromOption
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendRangeOption
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Permission
import com.streamr.client.rest.Stream
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.UnencryptedGroupKey
import org.apache.commons.codec.binary.Hex
import org.java_websocket.enums.ReadyState
import spock.util.concurrent.PollingConditions

import java.security.SecureRandom

class StreamrWebsocketSpec extends StreamrIntegrationSpecification {

	private SecureRandom secureRandom = new SecureRandom()
	private StreamrClient publisher
	private StreamrClient subscriber
	private Stream stream
	PollingConditions within10sec = new PollingConditions(timeout: 10)

	UnencryptedGroupKey genKey() {
		byte[] keyBytes = new byte[32]
		secureRandom.nextBytes(keyBytes)
		return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes), new Date())
	}

	void setup() {
		publisher = createClientWithPrivateKey(generatePrivateKey())
		subscriber = createClientWithPrivateKey(generatePrivateKey())

		stream = publisher.createStream(new Stream(generateResourceName(), ""))
		publisher.grant(stream, Permission.Operation.stream_get, subscriber.getPublisherId())
		publisher.grant(stream, Permission.Operation.stream_subscribe, subscriber.getPublisherId())
	}

	void cleanup() {
		if (publisher != null && publisher.state != ReadyState.CLOSED) {
			publisher.disconnect()
		}
		if (subscriber != null && subscriber.state != ReadyState.CLOSED) {
			subscriber.disconnect()
		}
	}

	void "client can connect and disconnect over websocket"() {
		when:
		publisher.connect()

		then:
		publisher.state == ReadyState.OPEN

		when:
		publisher.disconnect()

		then:
		publisher.state == ReadyState.CLOSED
	}


	void "client automatically connects for publishing"() {
		Stream stream = publisher.createStream(new Stream(generateResourceName(), ""))

		when:
		publisher.publish(stream, [foo: "bar"], new Date())

		then:
		publisher.state == ReadyState.OPEN
	}

	void "client automatically connects for subscribing"() {
		Stream stream = subscriber.createStream(new Stream(generateResourceName(), ""))

		when:
		subscriber.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {}
		})

		then:
		subscriber.state == ReadyState.OPEN
	}

	void "a subscriber receives published messages"() {
		int msgCount = 0
		Subscription sub

		when:
		// Subscribe to the stream
		StreamMessage latestMsg
		sub = subscriber.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				msgCount++
				latestMsg = message
			}

		})
		Thread.sleep(2000)

		// Produce messages to the stream
		for (int i=1; i<=10; i++) {
			publisher.publish(stream, [i: i])
			Thread.sleep(200)
		}

		then:
		// All messages have been received by subscriber
		within10sec.eventually {
			msgCount == 10
		}
		latestMsg.content.i == 10

		when:
		subscriber.unsubscribe(sub)

		then:
		within10sec.eventually {
			!sub.isSubscribed()
		}

		when:
		publisher.publish(stream, [i: 11])

		then:
		// No more messages should be received, since we're unsubscribed
		msgCount == 10
	}

	void "subscriber receives signed message if published with signature"() {
		when:
		// Subscribe to the stream
		StreamMessageV31 msg
		subscriber.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification didn't throw
				msg = (StreamMessageV31) message
			}
		})

		Thread.sleep(2000)

		publisher.publish(stream, [test: 'signed'])

		then:
		within10sec.eventually {
			msg != null
		}
		msg.getPublisherId() == publisher.getPublisherId()
		msg.signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
		msg.signature != null
	}

	void "subscriber can decrypt messages when he knows the keys used to encrypt"() {
		UnencryptedGroupKey key = genKey()
		HashMap<String, UnencryptedGroupKey> keys = new HashMap<>()
		keys.put(publisher.getPublisherId(), key)

		when:
		// Subscribe to the stream
		StreamMessageV31 msg
		subscriber.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification and decryption didn't throw
				msg = (StreamMessageV31) message
			}
		}, null, keys)
		Thread.sleep(2000)

		publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

		then:
		within10sec.eventually {
			msg != null && msg.getContent() == [test: 'clear text']
		}

		when:
		// publishing a second message with a new group key
		publisher.publish(stream, [test: 'another clear text'], new Date(), null, genKey())

		then:
		// no need to explicitly give the new group key to the subscriber
		within10sec.eventually {
			msg.getContent() == [test: 'another clear text']
		}
	}

	void "subscriber can get the group key and decrypt encrypted messages using an RSA key pair"() {
		UnencryptedGroupKey key = genKey()

		when:
		// Subscribe to the stream without knowing the group key
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		subscriber.subscribe(stream, new MessageHandler() {
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

		publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

		then:
		within10sec.eventually {
			assert msg1 != null
			// the subscriber got the group key and can decrypt
			assert msg1.getContent() == [test: 'clear text']
		}

		when:
		// publishing a second message with a new group key
		publisher.publish(stream, [test: 'another clear text'], new Date(), null, genKey())

		then:
		within10sec.eventually {
			assert msg2 != null
			// no need to explicitly give the new group key to the subscriber
			msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscriber can get the new group key after reset and decrypt encrypted messages"() {
		UnencryptedGroupKey key = genKey()

		when:
		// Subscribe to the stream without knowing the group key
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		subscriber.subscribe(stream, new MessageHandler() {
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

		publisher.publish(stream, [test: 'clear text'], new Date(), null, key)

		then:
		within10sec.eventually {
			assert msg1 != null
			// the subscriber got the group key and can decrypt
			assert msg1.getContent() == [test: 'clear text']
		}

		when:
		// publishing a second message after a rekey to revoke old subscribers
		publisher.rekey(stream)
		publisher.publish(stream, [test: 'another clear text'], new Date(), null)

		then:
		within10sec.eventually {
			assert msg2 != null
			// no need to explicitly give the new group key to the subscriber
			msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscriber can get the historical keys and decrypt old encrypted messages using an RSA key pair"() {
		// publishing historical messages with different group keys before subscribing
		publisher.publish(stream, [test: 'clear text'], new Date(), null, genKey())
		publisher.publish(stream, [test: 'another clear text'], new Date(), null, genKey())
		Thread.sleep(3000)

		when:
		// Subscribe to the stream with resend last without knowing the group keys
		StreamMessage msg1 = null
		StreamMessage msg2 = null
		subscriber.subscribe(stream, 0, new MessageHandler() {
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
		within10sec.eventually {
			assert msg1 != null && msg2 != null
			// the subscriber got the group keys and can decrypt the old messages
			assert msg1.getContent() == [test: 'clear text']
			assert msg2.getContent() == [test: 'another clear text']
		}
	}

	void "subscribe with resend last"() {
		Subscription sub
		boolean received = false

		when:
		publisher.publish(stream, [i: 1])
		Thread.sleep(6000) // wait to land in storage
		// Subscribe to the stream
		sub = subscriber.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				received = message.getContent() == [i: 1]
			}
		}, new ResendLastOption(1))

		Thread.sleep(6000)

		then:
		received
	}

	void "subscribe with resend from"() {
        boolean received = false
        boolean done = false

        when:
        publisher.publish(stream, [i: 1])
		Thread.sleep(2000)

        // Subscribe to the stream
        Subscription sub = subscriber.subscribe(stream, 0, new MessageHandler() {
            @Override
            void onMessage(Subscription s, StreamMessage message) {
                received = message.getContent() == [i: 1]
            }
			void done(Subscription sub) {
                done = true
            }
        }, new ResendFromOption(new Date(0)))

        then:
        within10sec.eventually() {
            assert done
            assert received
        }
	}

	void "resend last"() {
		List receivedMsg = []
		boolean done = false

		when:
		for (int i = 0; i <= 10; i++) {
			publisher.publish(stream, [i: i])
		}
		Thread.sleep(6000) // wait to land in storage

		int i = 0
		// Resend last
		subscriber.resend(stream, 0, new MessageHandler() {
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

		within10sec.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}

	void "resend from"() {
		List receivedMsg = []
		boolean done = false
		Date resendFromDate

		when:
		for (int i = 0; i <= 10; i++) {
			publisher.publish(stream, [i: i])

			if (i == 7) {
				resendFromDate = new Date()
			}
		}
		Thread.sleep(6000) // wait to land in storage

		int i = 0
		// Resend from
		subscriber.resend(stream, 0, new MessageHandler() {
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

		within10sec.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}

	void "resend range"() {
		List receivedMsg = []
		boolean done = false
		Date resendFromDate
		Date resendToDate

		when:
		for (int i = 0; i <= 10; i++) {
			Date date = new Date()
			publisher.publish(stream, [i: i], date)

			if (i == 3) {
				resendFromDate = new Date(date.getTime() + 1)
			}

			if (i == 7) {
				resendToDate = new Date(date.getTime() - 1)
			}
		}
		Thread.sleep(6000) // wait to land in storage

		// Resend range
		subscriber.resend(stream, 0, new MessageHandler() {
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

		within10sec.eventually() {
			assert done
			assert receivedMsg == expectedMessages
		}
	}
}
