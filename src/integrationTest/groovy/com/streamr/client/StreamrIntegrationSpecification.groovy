package com.streamr.client

import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    private final static DEFAULT_REST_URL = "http://localhost:8081/streamr-core/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost:8890/api/v1/ws?controlLayerVersion=1&messageLayerVersion=30"

    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Hex.encodeHexString(array)
    }

    protected static StreamrClient createClient(String apiKey = null) {
        return new StreamrClient(createOptions(apiKey))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClientOptions createOptions(String apiKey = null) {
        return new StreamrClientOptions(apiKey, DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        return new StreamrClientOptions(null, DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL, privateKey)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }

}
