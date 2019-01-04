package com.streamr.client

import com.streamr.client.protocol.StreamMessage
import com.streamr.client.rest.Stream

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

	void "client can subscribe to and unsubscribe from a public stream"() {
		client.connect()

		int msgCount = 0
		int timeout = 10 * 1000
		Subscription sub

		Stream stream = client.createStream(new Stream(generateResourceName(), ""))

		when:
		// Subscribe to the stream
		sub = client.subscribe(stream, new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				msgCount++
			}
		})

		Thread.sleep(2000)

		// Produce messages to the stream
		for (int i=0; i<10; i++) {
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
	}

}
