package com.streamr.client

import spock.lang.Specification

class StreamrWebsocketClientSpec extends Specification {

	private final static url = "wss://www.streamr.com/api/v1/ws" //"ws://localhost:8890/api/v1/ws";
	private StreamrWebsocketClient client

	void setup() {

	}

	void "client can connect and disconnect"() {
		client = new StreamrWebsocketClient()

		when:
		client.connect()

		then:
		client.state == StreamrWebsocketClient.State.Connected

		when:
		client.disconnect()

		then:
		client.state == StreamrWebsocketClient.State.Disconnected
	}



}
