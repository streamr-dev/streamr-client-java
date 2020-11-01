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
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.generated.Bytes32;
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
import java.util.Arrays;
import java.util.Optional;

import static com.streamr.client.utils.Web3jUtils.*;
import com.streamr.client.utils.Web3jUtils.Condition;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Bytes;
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
    private Credentials mainnetCred, sidechainCred;
    private String sidechainFactory, mainnetFactory;

    public DataUnionClient(String mainnet_url,
                           String mainnetFactory_,
                           Credentials mainnetCred_,
                           String sidechain_url,
                           String sidechainFactory_,
                           Credentials sidechainCred_
                           ) {
        mainnet = Web3j.build(new HttpService(mainnet_url));
        mainnetFactory = mainnetFactory_;
        mainnetCred = mainnetCred_;
        sidechain = Web3j.build(new HttpService(sidechain_url));
        sidechainFactory = sidechainFactory_;
        sidechainCred = sidechainCred_;
    }

    protected ContractGasProvider mainnetGasProvider() {
        return new EstimatedGasProvider(mainnet);
    }

    protected ContractGasProvider sidechainGasProvider() {
        return new EstimatedGasProvider(sidechain);
    }


    public Web3j getMainnetConnector(){
        return mainnet;
    }
    public Web3j getSidechainConnector(){
        return sidechain;
    }

    public DataUnionFactoryMainnet factoryMainnet() {
        return DataUnionFactoryMainnet.load(mainnetFactory, mainnet, mainnetCred, mainnetGasProvider());
    }
    public DataUnionMainnet mainnetDU(String address) {
        return DataUnionMainnet.load(address, mainnet, mainnetCred, mainnetGasProvider());
    }
    public DataUnionFactorySidechain factorySidechain() {
        return DataUnionFactorySidechain.load(sidechainFactory, sidechain, sidechainCred, sidechainGasProvider());
    }
    public DataUnionSidechain sidechainDU(String address) {
        return DataUnionSidechain.load(address, sidechain, sidechainCred, sidechainGasProvider());
    }

    public IERC20 mainnetToken() throws Exception {
        String tokenAddress = factoryMainnet().token().send().getValue();
        return IERC20.load(tokenAddress, mainnet, mainnetCred, mainnetGasProvider());
    }

    public IERC20 sidechainToken() throws Exception {
        String tokenAddress = factorySidechain().token().send().getValue();
        return IERC20.load(tokenAddress, sidechain, sidechainCred, sidechainGasProvider());
    }

    public ForeignAMB mainnetAMB() throws Exception {
        String amb = factoryMainnet().token().send().getValue();
        return ForeignAMB.load(amb, mainnet, mainnetCred, mainnetGasProvider());
    }

    public HomeAMB sidechainAMB() throws Exception {
        String amb = factorySidechain().token().send().getValue();
        return HomeAMB.load(amb, sidechain, sidechainCred, sidechainGasProvider());
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
        return (BigInteger) waitForCondition(earningsChange, sleeptime, timeout);
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


    /*
    event CollectedSignatures(
            address authorityResponsibleForRelay,
            bytes32 messageHash,
            uint256 NumberOfCollectedSignatures
    );



    from foreign AMB hasEnoughValidSignatures(): Need to construct this binary blob:

        * @param _signatures bytes blob with signatures to be validated.
    * First byte X is a number of signatures in a blob,
    * next X bytes are v components of signatures,
    * next 32 * X bytes are r components of signatures,
    * next 32 * X bytes are s components of signatures.
    */

    public void portTxToMainnet(byte[] msgHash, byte collectedSignatures) throws Exception {
        Bytes32 mhash = new Bytes32(msgHash);
        DynamicBytes message = sidechainAMB().message(mhash).send();
        byte[] signatures = new byte[1 + (65*collectedSignatures)];
        signatures[0] = collectedSignatures;
        //collect signatures one by one from sidechain, add to blob
        for(byte i = 0; i < collectedSignatures; i++){
            Sign.SignatureData sig = fromBytes65(sidechainAMB().signature(mhash, new Uint256(i)).send().getValue());
            signatures[i+1] = sig.getV()[0];
            System.arraycopy(sig.getR(), 0, signatures, 1+collectedSignatures+(i*32), 32);
            System.arraycopy(sig.getS(), 0, signatures, 1+(collectedSignatures*33)+(i*32), 32);
        }
        mainnetAMB().executeSignatures(message, new DynamicBytes(signatures)).send();
    }



    //utility functions:
    public byte[] signWithdrawAll(Credentials account, String recipient, String sidechainAddress) throws Exception {
        return signWithdraw(account, recipient, sidechainAddress,  BigInteger.ZERO);
    }

    public byte[] signWithdraw(Credentials account, String recipient, String sidechainAddress, BigInteger amount) throws Exception {
        Uint256 withdrawn = sidechainDU(sidechainAddress).getWithdrawn(new Address(account.getAddress())).send();
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