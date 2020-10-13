package com.streamr.client.utils;

import com.streamr.client.dataunion.DataUnionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Web3jUtils {
    private static final Logger log = LoggerFactory.getLogger(Web3jUtils.class);

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
