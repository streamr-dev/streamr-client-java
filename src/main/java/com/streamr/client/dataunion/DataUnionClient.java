package com.streamr.client.dataunion;

import com.google.common.collect.Lists;
import com.streamr.client.StreamrClient;
import com.streamr.client.dataunion.contracts.*;
import com.streamr.client.utils.Web3jUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;

import static com.streamr.client.utils.Web3jUtils.waitForCodeAtAddress;
import static com.streamr.client.utils.Web3jUtils.waitForErc20BalanceChange;
import static com.streamr.client.utils.Web3jUtils.waitForCondition;
import com.streamr.client.utils.Web3jUtils.Condition;


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

    public Web3j getMainnetConnector(){
        return mainnet;
    }
    public Web3j getSidechainConnector(){
        return sidechain;
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

    public IERC20 mainnetToken(String mainnetFactory, Credentials creds) throws Exception {
        String tokenAddress = factoryMainnet(mainnetFactory, creds).token().send().getValue();
        return IERC20.load(tokenAddress, mainnet, creds, new EstimatedGasProvider(mainnet));
    }

    public IERC20 sidechainToken(String sidechainFactory, Credentials creds) throws Exception {
        String tokenAddress = factorySidechain(sidechainFactory, creds).token().send().getValue();
        return IERC20.load(tokenAddress, sidechain, creds, new EstimatedGasProvider(sidechain));
    }

    public String waitForSidechainContract(String sidechainAddress, long sleeptime, long timeout) throws Exception {
        return waitForCodeAtAddress(sidechainAddress, sidechain, sleeptime, timeout);
    }

    public BigInteger waitForSidechainEarningsChange(final BigInteger initialBalance, DataUnionSidechain duSidechain, long sleeptime, long timeout) throws Exception {
        Condition earningsChange = new Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger bal = duSidechain.totalEarnings().send().getValue();
                if(!bal.equals(initialBalance))
                    return bal;
                return null;
            }
        };
        Object o = waitForCondition(earningsChange, sleeptime, timeout);
        return o == null ? null : (BigInteger) o;
    }

    public BigInteger waitForSidechainBalanceChange(BigInteger initialBalance, String tokenAddress, String balanceAddress, long sleeptime, long timeout) throws Exception {
        return waitForErc20BalanceChange(initialBalance, tokenAddress, balanceAddress, sidechain, sleeptime, timeout);
    }

    public BigInteger waitForMainnetBalanceChange(BigInteger initialBalance, String tokenAddress, String balanceAddress, long sleeptime, long timeout) throws Exception {
        return waitForErc20BalanceChange(initialBalance, tokenAddress, balanceAddress, mainnet, sleeptime, timeout);
    }

}