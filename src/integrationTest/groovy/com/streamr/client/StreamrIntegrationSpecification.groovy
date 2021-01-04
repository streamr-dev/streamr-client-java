package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.web3j.utils.Numeric
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {
    protected final static DEFAULT_REST_URL = "http://localhost/api/v1"
    protected final static DEFAULT_WEBSOCKET_URL = "ws://localhost/api/v1/ws"
    protected final static DEV_MAINCHAIN_RPC = "http://localhost:8545"
    protected final static DEV_SIDECHAIN_RPC = "http://localhost:8546"
    protected final static DEV_SIDECHAIN_FACTORY = "0x4081B7e107E59af8E82756F96C751174590989FE"
    protected final static DEV_MAINCHAIN_FACTORY = "0x5E959e5d5F3813bE5c6CeA996a286F734cc9593b"

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
