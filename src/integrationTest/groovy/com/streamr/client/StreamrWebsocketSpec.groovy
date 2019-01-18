package com.streamr.client

import com.streamr.client.rest.Stream
import com.streamr.client.protocol.message_layer.StreamMessage

class StreamrWebsocketSpec extends StreamrIntegrationSpecification {

	private StreamrClient client

	void setup() {
		client = createClient("tester1-api-key")
	}

	void cleanup() {
		if (client != null && client.state != StreamrWebsocketClient.State.Disconnected) {
			client.disconnect()
		}
	}

	void "client can connect and disconnect over websocket"() {
		when:
		client.connect()

		then:
		client.state == StreamrWebsocketClient.State.Connected

		when:
		client.disconnect()

		then:
		client.state == StreamrWebsocketClient.State.Disconnected
	}


	void "client automatically connects for publishing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.publish(stream, [foo: "bar"], new Date())

		then:
		client.state == StreamrWebsocketClient.State.Connected
	}

	void "client automatically connects for subscribing"() {
		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {}
		})

		then:
		client.state == StreamrWebsocketClient.State.Connected
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
		latestMsg.payload.i == 10

		when:
		client.unsubscribe(sub)
		Thread.sleep(2000)
		client.publish(stream, [i: 11])

		then:
		// No more messages should be received, since we're unsubscribed
		msgCount == 10
	}

}
