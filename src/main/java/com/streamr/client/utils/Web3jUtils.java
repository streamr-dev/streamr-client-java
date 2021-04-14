package com.streamr.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class Web3jUtils {
    private static final Logger log = LoggerFactory.getLogger(Web3jUtils.class);

    public interface Condition{
        //returns null if still waiting
        Object check() throws Exception;
    }

    public static class CodePresent implements Condition {
        private final Web3j connector;
        private final String address;

        @Override
        public Object check() throws IOException {
            log.info("Checking for code at " + address);
            String code = connector.ethGetCode(address, DefaultBlockParameterName.LATEST).send().getCode();
            if(code != null && !code.equals("0x")){
                log.info("Found code at " + address);
                return code;
            }
            return null;
        }

        public CodePresent(String address, Web3j connector){
            this.address = address;
            this.connector = connector;
        }
    }

    public static class Erc20BalanceChanged implements Condition {
        private final Web3j connector;
        private final String tokenAddress, balanceAddress;
        private final BigInteger initialBalance;

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

        public Erc20BalanceChanged(BigInteger initialBalance, String tokenAddress, String balanceAddress, Web3j connector){
            this.initialBalance = initialBalance;
            this.tokenAddress = tokenAddress;
            this.balanceAddress = balanceAddress;
            this.connector = connector;
        }
    }

    public static Object waitForCondition(Condition condition, final long sleeptime, final long timeout) throws Exception {
        return waitForCondition(condition, sleeptime, timeout, null);
    }

    public static Object waitForCondition(Condition condition, final long sleeptime, final long timeout, Exception timeoutException) throws Exception {
        if(sleeptime <= 0 || timeout <= 0) {
            return condition.check();
        }
        long slept = 0;
        while(slept < timeout){
            Object o = condition.check();
            if(o != null) {
                return o;
            }
            log.info("sleeping " + sleeptime + "ms");
            Thread.sleep(sleeptime);
            slept += sleeptime;
        }
        log.info("Timed out after " + timeout+ "ms");
        if (timeoutException != null) {
            throw timeoutException;
        } else {
            return null;
        }
    }

    public static DynamicArray<org.web3j.abi.datatypes.Address> asDynamicAddressArray(String[] addresses){
        ArrayList<org.web3j.abi.datatypes.Address> addressList = new ArrayList<org.web3j.abi.datatypes.Address>(addresses.length);
        for (String address : addresses) {
            addressList.add(new org.web3j.abi.datatypes.Address(address));
        }
        return new DynamicArray<org.web3j.abi.datatypes.Address>(Address.class, addressList);
    }


    public static String waitForCodeAtAddress(String address, Web3j connector, long sleeptime, long timeout) throws Exception {
        return (String) waitForCondition(new CodePresent(address, connector), sleeptime, timeout);
    }

    public static Boolean waitForTx(Web3j web3j, String txhash, long sleeptime, long timeout) throws Exception {
        Condition txfinish = new Condition(){
            @Override
            public Object check() throws Exception {
                Optional<TransactionReceipt> tr = web3j.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
                if(!tr.isPresent()) {
                    return null;
                }
                return tr.get().isStatusOK();
            }
        };
        return (Boolean) waitForCondition(txfinish, sleeptime, timeout);
    }

    /**
     *
     * waits until ERC20 balance changes from initialBalance
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

    public static BigInteger toWei(double ether){
        return new BigDecimal(BigInteger.TEN.pow(18)).multiply(BigDecimal.valueOf(ether)).toBigInteger();
    }
}
