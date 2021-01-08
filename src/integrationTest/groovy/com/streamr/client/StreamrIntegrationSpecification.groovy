package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.web3j.utils.Numeric
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification implements StreamrConstant {
    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Numeric.toHexString(array)
    }

    protected static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        return new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
