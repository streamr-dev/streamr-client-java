package com.streamr.client.dataunion;

import com.google.common.collect.Lists;
import com.streamr.client.StreamrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public String sidechainAddress(String mainnet_address) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        return callAddressGetterFunction(mainnet_address, "sidechainAddress", mainnet);
    }
    /*
     address public tokenMediator;
    address public dataUnionMainnet;

    uint256 public totalEarnings;
    uint256 public totalEarningsWithdrawn;

    uint256 public activeMemberCount;
    uint256 public lifetimeMemberEarnings;

    uint256 public joinPartAgentCount;

    uint256 public newMemberEth;
     */

    public String mainnetAddress(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callAddressGetterFunction(sidechain_address, "dataUnionMainnet", sidechain);
    }
    public BigInteger totalEarnings(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "totalEarnings", sidechain);
    }
    public BigInteger totalEarningsWithdrawn(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "totalEarningsWithdrawn", sidechain);
    }
    public BigInteger activeMemberCount(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "activeMemberCount", sidechain);
    }
    public BigInteger lifetimeMemberEarnings(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "lifetimeMemberEarnings", sidechain);
    }
    public BigInteger joinPartAgentCount(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "joinPartAgentCount", sidechain);
    }
    public BigInteger newMemberEth(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "newMemberEth", sidechain);
    }
    public BigInteger totalWithdrawable(String sidechain_address) throws NoSuchMethodException, InstantiationException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        return callUintGetterFunction(sidechain_address, "totalWithdrawable", sidechain);
    }

    /*
    public static BigInteger callUintGetterFunction(String contract, String functionName, Web3j connection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function fn = FunctionEncoder.makeFunction(functionName,
                Collections.<String>emptyList(),
                Collections.emptyList(),
                Arrays.<String>asList("uint256")
        );
        List<Type> ret = callFunction(contract, fn, connection);
        return (BigInteger) ret.iterator().next().getValue();
    }
    
     */



    public DUStats getDUStats(String sidechain_address){
        DUStats stats = new DUStats();
        return stats;
    }

    public MemberStats getMemberStats(String sidechain_address){
        MemberStats stats = new MemberStats();
        return stats;
    }

    public static BigInteger callUintGetterFunction(String contract, String functionName, Web3j connection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function fn = FunctionEncoder.makeFunction(functionName,
                Collections.<String>emptyList(),
                Collections.emptyList(),
                Arrays.<String>asList("uint256")
        );
        List<Type> ret = callFunction(contract, fn, connection);
        return (BigInteger) ret.iterator().next().getValue();
    }

    public static String callAddressGetterFunction(String contract, String functionName, Web3j connection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function fn = FunctionEncoder.makeFunction(functionName,
                Collections.<String>emptyList(),
                Collections.emptyList(),
                Arrays.<String>asList("address")
        );
        List<Type> ret = callFunction(contract, fn, connection);
        return (String) ret.iterator().next().getValue();
    }

    public static List<Type> callFunction(String contract, Function fn, Web3j connection) throws IOException {
        log.info("Calling view function " + fn + " on contract " + contract);
        EthCall response = connection.ethCall(
                Transaction.createEthCallTransaction(null, contract, FunctionEncoder.encode(fn)),
                DefaultBlockParameterName.LATEST).send();
        if (response.isReverted())
            return null;
        return FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
    }
}