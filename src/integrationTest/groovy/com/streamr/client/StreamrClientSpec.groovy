package com.streamr.client

import com.streamr.client.rest.Stream
import spock.lang.Specification

class StreamrClientSpec extends Specification {

	private final static url = "https://www.streamr.com/api/v1" //"https://localhost:8890/api/v1"
	private StreamrClient client

	void setup() {
		client = new StreamrClient(new StreamrClientOptions(null, null, url))
	}

	void cleanup() {

	}

	/*
	 * Stream endpoint tests
	 */

	void "getStream()"() {
		when:
		Stream s = client.getStream("7wa7APtlTq6EC5iTCBy6dw", null)

		then:
		s.id == "7wa7APtlTq6EC5iTCBy6dw"
	}

}
