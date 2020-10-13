package com.streamr.client.dataunion;

import com.google.common.collect.Lists;
import com.streamr.client.StreamrClient;
import com.streamr.client.dataunion.contracts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

/**
 * client for DU2
 */
public class DataUnionClient {
    private static final Logger log = LoggerFactory.getLogger(DataUnionClient.class);

    private Web3j mainnet, sidechain;

    public DataUnionClient(String mainnet_url, String sidechain_url) {
        mainnet = Web3j.build(new HttpService(mainnet_url));
        sidechain = Web3j.build(new HttpService(sidechain_url));
    }
    public DataUnionFactoryMainnet factoryMainnet(String address, Credentials creds) {
        return DataUnionFactoryMainnet.load(address, mainnet, creds, new EstimatedGasProvider(mainnet));
    }
    public DataUnionMainnet mainnetDU(String address, Credentials creds) {
        return DataUnionMainnet.load(address, mainnet, creds, new EstimatedGasProvider(mainnet));
    }
    public DataUnionFactorySidechain factorySidechain(String address, Credentials creds) {
        return DataUnionFactorySidechain.load(address, sidechain, creds, new EstimatedGasProvider(sidechain));
    }
    public DataUnionSidechain sidechainDU(String address, Credentials creds) {
        return DataUnionSidechain.load(address, sidechain, creds, new EstimatedGasProvider(sidechain));
    }



}