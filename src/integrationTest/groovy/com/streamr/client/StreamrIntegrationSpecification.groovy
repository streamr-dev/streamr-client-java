package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.testing.TestingMeta
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {


    protected static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), TestingMeta.WEBSOCKET_URL, TestingMeta.REST_URL))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        return new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), TestingMeta.WEBSOCKET_URL, TestingMeta.REST_URL)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
