package com.streamr.client.dataunion;

import com.google.common.collect.Lists;
import com.streamr.client.StreamrClient;
import com.streamr.client.dataunion.contracts.*;
import com.streamr.client.utils.Web3jUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.streamr.client.utils.Web3jUtils.*;
import static org.web3j.tx.Contract.staticExtractEventParameters;

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
        String amb = factoryMainnet().amb().send().getValue();
        return ForeignAMB.load(amb, mainnet, mainnetCred, mainnetGasProvider());
    }

    public HomeAMB sidechainAMB() throws Exception {
        String amb = factorySidechain().amb().send().getValue();
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

    public BigInteger waitForSidechainAMBAffirmations(final Bytes32 msgId, long sleeptime, long timeout) throws Exception {
        Condition affirmationsChange = new Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger requiredSignatures = sidechainAMB().requiredSignatures().send().getValue();
                //bit 255 is set in AMB to indicate completion
                BigInteger signatures = sidechainAMB().numMessagesSigned(msgId).send().getValue().clearBit(255);
                if(signatures.compareTo(requiredSignatures) < 0)
                    return null;
                return signatures;
            }
        };
        return (BigInteger) waitForCondition(affirmationsChange, sleeptime, timeout);
    }

    public BigInteger waitForSidechainBalanceChange(BigInteger initialBalance, String balanceAddress, long sleeptime, long timeout) throws Exception {
        return waitForErc20BalanceChange(initialBalance, sidechainToken().getContractAddress(), balanceAddress, sidechain, sleeptime, timeout);
    }

    public BigInteger waitForMainnetBalanceChange(BigInteger initialBalance, String balanceAddress, long sleeptime, long timeout) throws Exception {
        return waitForErc20BalanceChange(initialBalance, mainnetToken().getContractAddress(), balanceAddress, mainnet, sleeptime, timeout);
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


    java byte is signed, so we use short to support 0-255
    */
    public void portTxToMainnet(Bytes32 mhash, short collectedSignatures) throws Exception {
        if(collectedSignatures > 255)
            throw new UnsupportedOperationException("collectedSignatures cannot be greater than 255");

        DynamicBytes message = sidechainAMB().message(mhash).send();
        byte[] signatures = new byte[1 + (65*collectedSignatures)];
        signatures[0] = (byte) collectedSignatures;
        //collect signatures one by one from sidechain, add to blob
        for(short i = 0; i < collectedSignatures; i++){
            Sign.SignatureData sig = fromBytes65(sidechainAMB().signature(mhash, new Uint256(i)).send().getValue());
            signatures[i+1] = sig.getV()[0];
            System.arraycopy(sig.getR(), 0, signatures, 1+collectedSignatures+(i*32), 32);
            System.arraycopy(sig.getS(), 0, signatures, 1+(collectedSignatures*33)+(i*32), 32);
        }
        mainnetAMB().executeSignatures(message, new DynamicBytes(signatures)).send();
    }

    public void portTxsToMainnet(TransactionReceipt withdraw) throws Exception {
        List<Bytes32[]> msgs = extractAmbMessageIds(withdraw);
        for(Bytes32[] msg : msgs){
            Bytes32 id = msg[0];
            Bytes32 hash = msg[1];
            if(mainnetAMB().messageCallStatus(id).send().getValue() ||
                    !mainnetAMB().failedMessageSender(id).send().toUint().getValue().equals(BigInteger.ZERO)){
                log.warn("ForeignAMB has already seen msgId " + id + ". skipping");
                continue;
            }


            BigInteger signatures = waitForSidechainAMBAffirmations(hash, 10000, 600000);
            if(signatures == null){
                log.warn("Couldnt find affimation for AMB msgId " + id);
                continue;
            }
            log.info("Porting msgId " + id);
            portTxToMainnet(hash, signatures.shortValueExact());
        }
    }




    //utility functions:

    //create unsigned blob. must be signed to submit
    public byte[] createWithdrawAllRequest(String from, String to, String sidechainDUAddress) throws Exception {
        return createWithdrawRequest(from, to, sidechainDUAddress, BigInteger.ZERO);
    }

    public byte[] createWithdrawRequest(String from, String to, String sidechainDUAddress, BigInteger amount) throws Exception {
        Uint256 withdrawn = sidechainDU(sidechainDUAddress).getWithdrawn(new Address(from)).send();
        //TypeEncode doesnt expose a non-padding encode() :(
        String messageHex = TypeEncoder.encode(new Address(to)).substring(24) +
                TypeEncoder.encode(new Uint256(amount)) +
                TypeEncoder.encode(new Address(sidechainDUAddress)).substring(24) +
                TypeEncoder.encode(withdrawn);
        return Numeric.hexStringToByteArray(messageHex);
    }

    public byte[] signWithdraw(Credentials account, byte[] req) throws Exception {
        Sign.SignatureData sig = Sign.signPrefixedMessage(req, account.getEcKeyPair());
        return toBytes65(sig);
    }

    public static boolean isMemberActive(DataUnionSidechain dus, Address member) throws Exception {
        return STATUS_ACTIVE == dus.memberData(member).send().component1().getValue().longValue();
    }



    public static List<Bytes32[]> extractAmbMessageIds(TransactionReceipt tx){
        ArrayList<Bytes32[]> msgs = new ArrayList<Bytes32[]>();
        for(Log l : tx.getLogs()){
            EventValues vals = staticExtractEventParameters(HomeAMB.USERREQUESTFORSIGNATURE_EVENT, l);
            if(vals == null)
                continue;
            //event UserRequestForSignature(bytes32 indexed messageId, bytes encodedData);
            Bytes32[] idAndHash = new Bytes32[2];
            idAndHash[0] = (Bytes32) vals.getIndexedValues().get(0);
            idAndHash[1] = new Bytes32(Hash.sha3(((DynamicBytes) vals.getNonIndexedValues().get(0)).getValue()));
            msgs.add(idAndHash);
        }
        return msgs;
    }


}