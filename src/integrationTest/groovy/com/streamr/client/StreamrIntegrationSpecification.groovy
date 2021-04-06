package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.dataunion.DataUnionClient
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    protected final static DEFAULT_REST_URL = "http://localhost/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost/api/v1/ws"

    protected final static DEV_MAINCHAIN_RPC = "http://localhost:8545"
    protected final static DEV_SIDECHAIN_RPC = "http://localhost:8546"
    protected final static DEV_SIDECHAIN_FACTORY = "0x4A4c4759eb3b7ABee079f832850cD3D0dC48D927"
    protected final static DEV_MAINCHAIN_FACTORY = "0x4bbcBeFBEC587f6C4AF9AF9B48847caEa1Fe81dA"

    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Hex.encodeHexString(array)
    }

    protected static DataUnionClient devChainDataUnionClient(String mainnetAdminPrvKey, String sidechainAdminPrvKey) {
        StreamrClientOptions opts = new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
        opts.setSidechainRpcUrl(DEV_SIDECHAIN_RPC)
        opts.setMainnetRpcUrl(DEV_MAINCHAIN_RPC)
        opts.setDataUnionMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY)
        opts.setDataUnionSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY)
        opts.setConnectionTimeoutMillis(60000)
        return new StreamrClient(opts).dataUnionClient(mainnetAdminPrvKey, sidechainAdminPrvKey)
    }

    protected static StreamrClient createUnauthenticatedClient() {
        StreamrClientOptions opts =  new StreamrClientOptions(null, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL);
        opts.setConnectionTimeoutMillis(60000)
        return new StreamrClient(opts)
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createOptionsWithPrivateKey(privateKey))
    }

    protected static StreamrClientOptions createOptionsWithPrivateKey(String privateKey = null) {
        StreamrClientOptions opts =  new StreamrClientOptions(new EthereumAuthenticationMethod(privateKey), SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
        opts.setConnectionTimeoutMillis(60000)
        return opts
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }
}
