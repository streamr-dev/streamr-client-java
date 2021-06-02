package com.streamr.client

import com.streamr.client.authentication.EthereumAuthenticationMethod
import com.streamr.client.dataunion.DataUnionClient
import com.streamr.client.options.DataUnionClientOptions
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import com.streamr.client.authentication.AuthenticationMethod
import com.streamr.client.rest.StorageNode
import com.streamr.client.utils.Address
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

class StreamrIntegrationSpecification extends Specification {

    protected final static DEFAULT_REST_URL = "http://localhost/api/v1"
    private final static DEFAULT_WEBSOCKET_URL = "ws://localhost/api/v1/ws"

    protected final static DEV_MAINCHAIN_RPC = "http://localhost:8545"
    protected final static DEV_SIDECHAIN_RPC = "http://localhost:8546"
    protected final static DEV_SIDECHAIN_FACTORY = "0x4A4c4759eb3b7ABee079f832850cD3D0dC48D927"
    protected final static DEV_MAINCHAIN_FACTORY = "0x4bbcBeFBEC587f6C4AF9AF9B48847caEa1Fe81dA"
    protected final static DEV_BINANCE_ADAPTER = "0xdc5F6368cd31330adC259386e78604a5E29E9415"

    // "broker-node-storage-1" om Docker environment
    protected final static DEV_STORAGE_NODE_ADDRESS = new Address("0xde1112f631486CfC759A50196853011528bC5FA0")
    protected final static DEV_STORAGE_NODE_URL = "http://10.200.10.1:8891"

    protected static String generatePrivateKey() {
        byte[] array = new byte[32]
        new Random().nextBytes(array)
        return Hex.encodeHexString(array)
    }

    protected static DataUnionClient devChainDataUnionClient(String mainnetAdminPrvKey, String sidechainAdminPrvKey) {
        DataUnionClientOptions opts = new DataUnionClientOptions(DEV_MAINCHAIN_RPC, mainnetAdminPrvKey, DEV_SIDECHAIN_RPC, sidechainAdminPrvKey);
        opts.setDataUnionMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY)
        opts.setDataUnionSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY)
        opts.setBinanceAdapterAddress(DEV_BINANCE_ADAPTER)
        return new DataUnionClient(opts)
    }

    protected static StreamrClient createUnauthenticatedClient() {
        return new StreamrClient(createDevOptions(null))
    }

    protected static StreamrClient createClientWithPrivateKey(String privateKey = null) {
        return new StreamrClient(createDevOptions(new EthereumAuthenticationMethod(privateKey)))
    }

    private static StreamrClientOptions createDevOptions(AuthenticationMethod authenticationMethod) {
        StreamrClientOptions opts = new StreamrClientOptions(authenticationMethod, SigningOptions.getDefault(), EncryptionOptions.getDefault(), DEFAULT_WEBSOCKET_URL, DEFAULT_REST_URL)
        opts.setConnectionTimeoutMillis(60000)
        opts.setStorageNodeAddress(DEV_STORAGE_NODE_ADDRESS)
        opts.setStorageNodeUrl(DEV_STORAGE_NODE_URL)
        return opts
    }

    protected String generateResourceName() {
        return "${this.getClass().getSimpleName()}-${System.currentTimeMillis()}"
    }

    protected StorageNode getDevStorageNode() {
        return new StorageNode(DEV_STORAGE_NODE_ADDRESS);
    }
}
