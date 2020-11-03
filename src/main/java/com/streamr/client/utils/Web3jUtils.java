package com.streamr.client.utils;

import com.streamr.client.dataunion.DataUnionClient;
import com.streamr.client.dataunion.contracts.IERC20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Web3jUtils {
    private static final Logger log = LoggerFactory.getLogger(Web3jUtils.class);

    public interface Condition{
        //returns null if still waiting
        Object check() throws Exception;
    }

    public static class CodePresent implements Condition {
        private Web3j connector;
        private String address;

        @Override
        public Object check() throws IOException {
            log.info("Checking for code at " + address);
            String code = connector.ethGetCode(address, DefaultBlockParameterName.LATEST).send().getCode();
            if(code != null && code != "0x"){
                log.info("Found code at " + address);
                return code;
            }
            return null;
        }

        public CodePresent(String address_, Web3j connector_){
            address = address_;
            connector = connector_;
        }
    }

    public static class Erc20BalanceChanged implements Condition {
        private Web3j connector;
        private String tokenAddress, balanceAddress;
        private BigInteger initialBalance;

        @Override
        public Object check() throws Exception {
            log.info("Checking ERC20 balance for  " + balanceAddress);
            BigInteger bal = erc20Balance(tokenAddress, balanceAddress, connector);
            if(!bal.equals(initialBalance)){
                log.info("Balance changed: " + bal);
                return bal;
            }
            return null;
        }

        public Erc20BalanceChanged(BigInteger initialBalance_, String tokenAddress_, String balanceAddress_, Web3j connector_){
            initialBalance = initialBalance_;
            tokenAddress = tokenAddress_;
            balanceAddress = balanceAddress_;
            connector = connector_;
        }
    }

    public static Object waitForCondition(Condition condition, long sleeptime, long timeout) throws Exception {
        if(sleeptime <= 0 || timeout <= 0)
            return null;
        long slept = 0;
        while(slept < timeout){
            Object o = condition.check();
            if(o != null)
                return o;
            log.info("sleeping " + timeout + "ms");
            Thread.sleep(sleeptime);
            slept += sleeptime;
        }
        return null;
    }

    public static String waitForCodeAtAddress(String address, Web3j connector, long sleeptime, long timeout) throws Exception {
        return (String) waitForCondition(new CodePresent(address, connector), sleeptime, timeout);
    }

    public static Boolean waitForTx(Web3j web3j, String txhash, long sleeptime, long timeout) throws Exception {
        Condition txfinish = new Condition(){
            @Override
            public Object check() throws Exception {
                Optional<TransactionReceipt> tr = web3j.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
                if(!tr.isPresent())
                    return null;
                return tr.get().isStatusOK();
            }
        };
        return (Boolean) waitForCondition(txfinish, sleeptime, timeout);
    }

    /**
     *
     * @param initialBalance
     * @param tokenAddress
     * @param balanceAddress
     * @param connector
     * @param sleeptime
     * @param timeout
     * @return new balance, or null if balance didnt change in waiting period
     * @throws Exception
     */

    public static BigInteger waitForErc20BalanceChange(BigInteger initialBalance, String tokenAddress, String balanceAddress, Web3j connector, long sleeptime, long timeout) throws Exception {
        Object o = waitForCondition(new Erc20BalanceChanged(initialBalance, tokenAddress, balanceAddress, connector), sleeptime, timeout);
        return o == null ? null : (BigInteger) o;
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

    public static BigInteger erc20Balance(String contract, String balanceAddress, Web3j connection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException {
        Function fn = FunctionEncoder.makeFunction("balanceOf",
                Arrays.<String>asList("address"),
                Arrays.asList(balanceAddress),
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

    // these methods should be built in to SignatureData
    public static byte[] toBytes65(Sign.SignatureData sig){
        byte[] result = new byte[65];
        System.arraycopy(sig.getR(), 0, result, 0, 32);
        System.arraycopy(sig.getS(), 0, result, 32, 32);
        System.arraycopy(sig.getV(), 0, result, 64, 1);
        return result;
    }

    public static Sign.SignatureData fromBytes65(byte[] bytes){
        byte[] r = Arrays.copyOfRange(bytes, 0, 32);
        byte[] s = Arrays.copyOfRange(bytes, 32, 64);
        byte[] v = Arrays.copyOfRange(bytes, 64, 65);
        return new Sign.SignatureData(v,r,s);
    }
}
