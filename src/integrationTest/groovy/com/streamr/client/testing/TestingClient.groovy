package com.streamr.client.testing

import com.streamr.client.StreamrClient
import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions

// TODO: Move code to TestingStreamrClient.java
final class TestingClient {
    private TestingClient() {}

    public static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), TestingMeta.WEBSOCKET_URL, TestingMeta.REST_URL))
    }

    public static StreamrClient createClientWithPrivateKey(String privateKey) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    private static StreamrClientOptions createOptionsWithPrivateKey(String privateKey) {
        return new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), TestingMeta.WEBSOCKET_URL, TestingMeta.REST_URL)
    }
}
