package com.streamr.client

import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    private final static DEFAULT_REST_URL = "http://localhost:8081/streamr-core/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost:8890/api/v1/ws";

    protected static StreamrClient createClient(String apiKey = null) {
        return new StreamrClient(createOptions(apiKey))
    }

    protected static StreamrClientOptions createOptions(String apiKey = null) {
        return new StreamrClientOptions(apiKey, DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }

}
