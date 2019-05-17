package com.streamr.client

import com.streamr.client.options.ResendFromOption
import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendRangeOption
import com.streamr.client.protocol.message_layer.StreamMessageV30
import com.streamr.client.rest.Stream
import com.streamr.client.protocol.message_layer.StreamMessage
import spock.util.concurrent.PollingConditions

class StreamrWebsocketSpec extends StreamrIntegrationSpecification {

	private StreamrClient client

	void setup() {
		client = createClientWithPrivateKey(generatePrivateKey())
	}

	void cleanup() {
		if (client != null && client.state != StreamrClient.State.Disconnected) {
			client.disconnect()
		}
	}

	void "client can connect and disconnect over websocket"() {
		when:
		client.connect()

		then:
		client.state == StreamrClient.State.Connected

		when:
		client.disconnect()

		then:
		client.state == StreamrClient.State.Disconnected
	}


	void "client automatically connects for publishing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.publish(stream, [foo: "bar"], new Date())

		then:
		client.state == StreamrClient.State.Connected
	}

	void "client automatically connects for subscribing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {}
		})

		then:
		client.state == StreamrClient.State.Connected
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
		StreamMessageV30 msg
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				//reaching this point ensures that the signature verification didn't throw
				msg = (StreamMessageV30) message
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

	void "subscribe with resend last"() {
		Subscription sub

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		boolean received = false

		when:
		client.publish(stream, [i: 1])
		Thread.sleep(2000)
		// Subscribe to the stream
		sub = client.subscribe(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				received = message.getContent() == [i: 1]
			}
		}, new ResendLastOption(1))

		Thread.sleep(5000)

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

		Map<String, Object>[] receivedMsg = new Map<String, Object>[5]
		boolean done = false

		when:
		for (int i = 0; i <= 10; i++) {
			client.publish(stream, [i: i])
		}
		Thread.sleep(2000)

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg[i++] = message.getContent()
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendLastOption(5))

		then:
		Map<String, Object>[] expectedMessages = [
			Collections.singletonMap("i", 6.0),
			Collections.singletonMap("i", 7.0),
			Collections.singletonMap("i", 8.0),
			Collections.singletonMap("i", 9.0),
			Collections.singletonMap("i", 10.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
			System.out.println("finished test")
		}
	}

	void "resend from"() {
		given:
		def conditions = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		Map<String, Object>[] receivedMsg = new Map<String, Object>[3]
		boolean done = false
		Date resendFromDate

		when:
		for (int i = 0; i <= 10; i++) {
			client.publish(stream, [i: i])

			if (i == 7) {
				resendFromDate = new Date()
			}
		}
		Thread.sleep(2000)

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg[i++] = message.getContent()
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendFromOption(resendFromDate))

		then:
		Map<String, Object>[] expectedMessages = [
				Collections.singletonMap("i", 8.0),
				Collections.singletonMap("i", 9.0),
				Collections.singletonMap("i", 10.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
			System.out.println("finished test")
		}
	}

	void "resend range"() {
		given:
		def conditions = new PollingConditions(timeout: 10)

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		Map<String, Object>[] receivedMsg = new Map<String, Object>[3]
		boolean done = false
		Date resendFromDate
		Date resendToDate

		when:
		for (int i = 0; i <= 10; i++) {
			client.publish(stream, [i: i])

			if (i == 3) {
				resendFromDate = new Date()
			}

			if (i == 7) {
				resendToDate = new Date()
			}
		}
		Thread.sleep(2000)

		int i = 0
		// Subscribe to the stream
		client.resend(stream, 0, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				receivedMsg[i++] = message.getContent()
			}
			void done(Subscription sub) {
				done = true
			}
		}, new ResendRangeOption(resendFromDate, resendToDate))

		then:
		Map<String, Object>[] expectedMessages = [
				Collections.singletonMap("i", 4.0),
				Collections.singletonMap("i", 5.0),
				Collections.singletonMap("i", 6.0)
		]

		conditions.eventually() {
			assert done
			assert receivedMsg == expectedMessages
			System.out.println("finished test")
		}
	}
}
