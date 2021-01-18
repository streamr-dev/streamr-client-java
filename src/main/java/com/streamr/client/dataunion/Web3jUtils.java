package com.streamr.client.dataunion;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

final class Web3jUtils {
  private static final Logger log = LoggerFactory.getLogger(Web3jUtils.class);

  interface Condition {
    // returns null if still waiting
    Object check() throws Exception;
  }

  static class CodePresent implements Condition {
    private final Web3j connector;
    private final String address;

    @Override
    public Object check() throws IOException {
      log.info("Checking for code at " + address);
      String code =
          connector.ethGetCode(address, DefaultBlockParameterName.LATEST).send().getCode();
      if (code != null && !code.equals("0x")) {
        log.info("Found code at " + address);
        return code;
      }
      return null;
    }

    CodePresent(final String address, final Web3j connector) {
      this.address = address;
      this.connector = connector;
    }
  }

  static class Erc20BalanceChanged implements Condition {
    private final Web3j connector;
    private final String tokenAddress, balanceAddress;
    private final BigInteger initialBalance;

    @Override
    public Object check() throws Exception {
      log.info("Checking ERC20 balance for  " + balanceAddress);
      BigInteger bal = erc20Balance(tokenAddress, balanceAddress, connector);
      if (!bal.equals(initialBalance)) {
        log.info("Balance changed: " + bal);
        return bal;
      }
      return null;
    }

    Erc20BalanceChanged(
        final BigInteger initialBalance, final String tokenAddress, final String balanceAddress, final Web3j connector) {
      this.initialBalance = initialBalance;
      this.tokenAddress = tokenAddress;
      this.balanceAddress = balanceAddress;
      this.connector = connector;
    }
  }

  static Object waitForCondition(
      final Condition condition, final long sleeptime, final long timeout) throws Exception {
    if (sleeptime <= 0 || timeout <= 0) {
      return condition.check();
    }
    long slept = 0;
    while (slept < timeout) {
      Object o = condition.check();
      if (o != null) {
        return o;
      }
      log.info("sleeping " + sleeptime + "ms");
      Thread.sleep(sleeptime);
      slept += sleeptime;
    }
    log.info("Timed out after " + timeout + "ms");
    return null;
  }

  static DynamicArray<org.web3j.abi.datatypes.Address> asDynamicAddressArray(
      final String[] addresses) {
    ArrayList<Address> addressList =
        new ArrayList<org.web3j.abi.datatypes.Address>(addresses.length);
    for (String address : addresses) {
      addressList.add(new org.web3j.abi.datatypes.Address(address));
    }
    return new DynamicArray<org.web3j.abi.datatypes.Address>(Address.class, addressList);
  }

  static String waitForCodeAtAddress(
      final String address, final Web3j connector, final long sleeptime, final long timeout) throws Exception {
    return (String) waitForCondition(new CodePresent(address, connector), sleeptime, timeout);
  }

  static Boolean waitForTx(final Web3j web3j, final String txhash, final long sleeptime, final long timeout)
      throws Exception {
    Condition txfinish =
        new Condition() {
          @Override
          public Object check() throws Exception {
            Optional<TransactionReceipt> tr =
                web3j.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
            if (!tr.isPresent()) {
              return null;
            }
            return tr.get().isStatusOK();
          }
        };
    return (Boolean) waitForCondition(txfinish, sleeptime, timeout);
  }

  /**
   * Waits until ERC20 balance changes from initialBalance
   *
   * @return new balance, or null if balance didnt change in waiting period
   */
  static BigInteger waitForErc20BalanceChange(
      final BigInteger initialBalance,
      final String tokenAddress,
      final String balanceAddress,
      final Web3j connector,
      final long sleeptime,
      final long timeout)
      throws Exception {
    Object o =
        waitForCondition(
            new Erc20BalanceChanged(initialBalance, tokenAddress, balanceAddress, connector),
            sleeptime,
            timeout);
    return o == null ? null : (BigInteger) o;
  }

  static BigInteger callUintGetterFunction(
      final String contract, final String functionName, final Web3j connection)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
          InstantiationException, IllegalAccessException, IOException {
    Function fn =
        FunctionEncoder.makeFunction(
            functionName,
            Collections.<String>emptyList(),
            Collections.emptyList(),
            Arrays.<String>asList("uint256"));
    @SuppressWarnings("rawtypes")
    List<Type> ret = callFunction(contract, fn, connection);
    return (BigInteger) ret.iterator().next().getValue();
  }

  static BigInteger erc20Balance(final String contract, final String balanceAddress, final Web3j connection)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
          InstantiationException, IllegalAccessException, IOException {
    Function fn =
        FunctionEncoder.makeFunction(
            "balanceOf",
            Arrays.<String>asList("address"),
            Arrays.asList(balanceAddress),
            Arrays.<String>asList("uint256"));
    @SuppressWarnings("rawtypes")
    List<Type> ret = callFunction(contract, fn, connection);
    return (BigInteger) ret.iterator().next().getValue();
  }

  @SuppressWarnings("rawtypes")
  static List<Type> callFunction(final String contract, final Function fn, final Web3j connection)
      throws IOException {
    log.info("Calling view function " + fn + " on contract " + contract);
    EthCall response =
        connection
            .ethCall(
                Transaction.createEthCallTransaction(null, contract, FunctionEncoder.encode(fn)),
                DefaultBlockParameterName.LATEST)
            .send();
    if (response.isReverted()) return null;
    return FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
  }

  // these methods should be built in to SignatureData
  static byte[] toBytes65(final Sign.SignatureData sig) {
    byte[] result = new byte[65];
    System.arraycopy(sig.getR(), 0, result, 0, 32);
    System.arraycopy(sig.getS(), 0, result, 32, 32);
    System.arraycopy(sig.getV(), 0, result, 64, 1);
    return result;
  }

  static Sign.SignatureData fromBytes65(final byte[] bytes) {
    byte[] r = Arrays.copyOfRange(bytes, 0, 32);
    byte[] s = Arrays.copyOfRange(bytes, 32, 64);
    byte[] v = Arrays.copyOfRange(bytes, 64, 65);
    return new Sign.SignatureData(v, r, s);
  }

  static BigInteger toWei(final double ether) {
    return new BigDecimal(BigInteger.TEN.pow(18))
        .multiply(BigDecimal.valueOf(ether))
        .toBigInteger();
  }
}
