package com.streamr.client

import spock.lang.Specification

class StreamrWebsocketClientSpec extends Specification {

	private final static url = "ws://localhost:8890/api/v1/ws";
	private StreamrWebsocketClient client;

	void setup() {

	}

	/*
	void "client can connect"() {
		when:
		client.connect();

		then:
		queue.toList() == [ExampleData.MESSAGE_2, ExampleData.MESSAGE_1, ExampleData.MESSAGE_2]
	}

	void "accept(message) invokes Stasts#onReadFromKafka"() {
		def queue = new ArrayBlockingQueue(3)
		def stats = Mock(Stats)
		QueueProducer queueProducer = new QueueProducer(queue, stats)

		when:
		queueProducer.accept(ExampleData.MESSAGE_2)
		queueProducer.accept(ExampleData.MESSAGE_1)
		queueProducer.accept(ExampleData.MESSAGE_2)

		then:
		1 * stats.onReadFromKafka(ExampleData.MESSAGE_1)
		2 * stats.onReadFromKafka(ExampleData.MESSAGE_2)
	}
	*/
}
