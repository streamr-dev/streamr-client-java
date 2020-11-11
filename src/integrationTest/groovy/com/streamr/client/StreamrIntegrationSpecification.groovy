package com.streamr.client

import com.streamr.client.authentication.ApiKeyAuthenticationMethod
import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.dataunion.DataUnionClient
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.apache.commons.codec.binary.Hex
import org.web3j.crypto.Credentials
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    protected final static DEFAULT_REST_URL = "http://localhost/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost/api/v1/ws"

    protected final static DEV_MAINCHAIN_RPC = "http://localhost:8545"
    protected final static DEV_SIDECHAIN_RPC = "http://localhost:8546"
    protected final static DEV_SIDECHAIN_FACTORY = "0x4081B7e107E59af8E82756F96C751174590989FE"
    protected final static DEV_MAINCHAIN_FACTORY = "0x5E959e5d5F3813bE5c6CeA996a286F734cc9593b"

    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Hex.encodeHexString(array)
    }

    protected static DataUnionClient devChainDataUnionClient(String mainnetAdminPrvKey, String sidechainAdminPrvKey) {
        StreamrClientOptions opts = new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
        opts.setSidechainRpcUrl(DEV_SIDECHAIN_RPC)
        opts.setMainnetRpcUrl(DEV_MAINCHAIN_RPC)
        opts.setMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY)
        opts.setSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY)
        return new StreamrClient(opts).dataUnionClient(mainnetAdminPrvKey, sidechainAdminPrvKey)
    }

    protected static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClient createClientWithApiKey(String apiKey = null) {
        return new StreamrClient(createOptionsWithApiKey(apiKey))
    }

    protected static StreamrClientOptions createOptionsWithApiKey(String apiKey = null) {
        return new StreamrClientOptions(new ApiKeyAuthenticationMethod(apiKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        return new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
