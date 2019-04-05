package com.streamr.client

import com.streamr.client.authentication.ApiKeyAuthenticationMethod
import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    protected final static DEFAULT_REST_URL = "http://localhost:8081/streamr-core/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost:8890/api/v1/ws?controlLayerVersion=1&messageLayerVersion=30"

    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Hex.encodeHexString(array)
    }

    protected static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(new StreamrClientOptions(null, SigningOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClient createClientWithApiKey(String apiKey = null) {
        return new StreamrClient(createOptionsWithApiKey(apiKey))
    }

    protected static StreamrClientOptions createOptionsWithApiKey(String apiKey = null) {
        return new StreamrClientOptions(new ApiKeyAuthenticationMethod(apiKey), SigningOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        return new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
