package com.streamr.client.dataunion;

import com.google.common.collect.Lists;
import com.streamr.client.StreamrClient;
import com.streamr.client.dataunion.contracts.*;
import com.streamr.client.utils.Web3jUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static com.streamr.client.utils.Web3jUtils.waitForCodeAtAddress;
import static com.streamr.client.utils.Web3jUtils.waitForErc20BalanceChange;
import static com.streamr.client.utils.Web3jUtils.waitForCondition;
import com.streamr.client.utils.Web3jUtils.Condition;
import org.web3j.utils.Numeric;


/**
 * client for DU2
 */
public class DataUnionClient {
    private static final Logger log = LoggerFactory.getLogger(DataUnionClient.class);

    public enum TxStatus{unconfirmed, success, error};
    public static final long STATUS_NONE = 0;
    public static final long STATUS_ACTIVE = 1;
    public static final long STATUS_INACTIVE = 2;

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

    public Boolean waitForMainnetTx(String txhash, long sleeptime, long timeout) throws Exception {
        return waitForTx(mainnet, txhash, sleeptime, timeout);
    }

    public Boolean waitForSidechainTx(String txhash, long sleeptime, long timeout) throws Exception {
        return waitForTx(sidechain, txhash, sleeptime, timeout);
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

    //utility functions:


    // this method should be built in to SignatureData
    public static byte[] toBytes65(Sign.SignatureData sig){
        byte[] result = new byte[65];
        System.arraycopy(sig.getR(), 0, result, 0, 32);
        System.arraycopy(sig.getS(), 0, result, 32, 32);
        System.arraycopy(sig.getV(), 0, result, 64, 1);
        return result;
    }
    /*
            bytes32 messageHash = keccak256(abi.encodePacked(
            "\x19Ethereum Signed Message:\n104", recipient, amount, address(this), getWithdrawn(signer)));
     */
    public byte[] signWithdrawAll(Credentials account, String recipient, String sidechainAddress) throws Exception {
        return signWithdraw(account, recipient, sidechainAddress,  BigInteger.ZERO);
    }

    public byte[] signWithdraw(Credentials account, String recipient, String sidechainAddress, BigInteger amount) throws Exception {

        Uint256 withdrawn = sidechainDU(sidechainAddress, account).getWithdrawn(new Address(account.getAddress())).send();

        //TypeEncode doesnt expose a non-padding encode() :(
        String messageHex = TypeEncoder.encode(new Address(recipient)).substring(24) +
                TypeEncoder.encode(new Uint256(amount)) +
                TypeEncoder.encode(new Address(sidechainAddress)).substring(24) +
                TypeEncoder.encode(withdrawn);
        System.out.println("Created withdrawal signature " + messageHex);

        Sign.SignatureData sig = Sign.signPrefixedMessage(Numeric.hexStringToByteArray(messageHex), account.getEcKeyPair());
        return toBytes65(sig);
    }

    public static boolean isMemberActive(DataUnionSidechain dus, Address member) throws Exception {
        return STATUS_ACTIVE == dus.memberData(member).send().component1().getValue().longValue();
    }


}