package com.streamr.client

import com.streamr.client.protocol.StreamMessage
import spock.lang.Specification

class StreamrWebsocketClientSpec extends Specification {

	private final static url = "wss://www.streamr.com/api/v1/ws" //"ws://localhost:8890/api/v1/ws";
	private StreamrWebsocketClient client

	void setup() {

	}

	void cleanup() {
		if (client != null && client.state != StreamrWebsocketClient.State.Disconnected) {
			client.disconnect()
		}
	}

	void "client can connect and disconnect"() {
		client = new StreamrWebsocketClient(new StreamrClientOptions(null, url, null))

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
		client = new StreamrWebsocketClient(new StreamrClientOptions(null, url, null))
		client.connect()
		int msgCount = 0
		int timeout = 10 * 1000
		Subscription sub

		when:
		sub = client.subscribe("7wa7APtlTq6EC5iTCBy6dw", new MessageHandler() {
			@Override
			void onMessage(Subscription s, StreamMessage message) {
				msgCount++
			}
		})
		while (msgCount == 0 && timeout > 0) {
			Thread.sleep(200)
			timeout -= 200
		}

		then:
		msgCount > 0
		timeout > 0

		when:
		client.unsubscribe(sub)
		int msgCountAfterUnsubscribe = msgCount
		Thread.sleep(5000)

		then: "No new messages are received after unsubscribe"
		msgCount == msgCountAfterUnsubscribe
	}

}
