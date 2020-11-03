package com.streamr.client.dataunion;

import com.streamr.client.dataunion.contracts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.streamr.client.utils.Web3jUtils.*;
import static org.web3j.tx.Contract.staticExtractEventParameters;

import com.streamr.client.utils.Web3jUtils.Condition;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;


/**
 * client for DU2
 */
public class DataUnionClient {
    private static final Logger log = LoggerFactory.getLogger(DataUnionClient.class);

    private Web3j mainnet, sidechain;
    private Credentials mainnetCred, sidechainCred;
    private String sidechainFactory, mainnetFactory;

    public DataUnionClient(String mainnet_url,
                           String mainnetFactory_,
                           Credentials mainnetAdmin,
                           String sidechain_url,
                           String sidechainFactory_,
                           Credentials sidechainAdmin
                           ) {
        mainnet = Web3j.build(new HttpService(mainnet_url));
        mainnetFactory = mainnetFactory_;
        mainnetCred = mainnetAdmin;
        sidechain = Web3j.build(new HttpService(sidechain_url));
        sidechainFactory = sidechainFactory_;
        sidechainCred = sidechainAdmin;
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

    public DataUnion deployNewDataUnion(String name, String admin, BigInteger adminFee, List<String> agents) throws Exception {
        ArrayList<Address> agentAddresses = new ArrayList<Address>(agents.size());
        for (String agent : agents) {
            agentAddresses.add(new Address(agent));
        }
        Utf8String duname = new Utf8String(name);
        factoryMainnet().deployNewDataUnion(
                new Address(admin),
                new Uint256(adminFee),
                new DynamicArray<Address>(Address.class, agentAddresses),
                duname
        ).send();
        Address mainnetAddress = factoryMainnet().mainnetAddress(new Address(mainnetCred.getAddress()), duname).send();
        String sidechainAddress = factoryMainnet().sidechainAddress(mainnetAddress).send().getValue();
        return new DataUnion(mainnetDU(mainnetAddress.getValue()), sidechainDU(sidechainAddress));
    }

    public DataUnion dataUnionFromName(String name) throws Exception {
        String address = factoryMainnet().mainnetAddress(new Address(mainnetCred.getAddress()), new Utf8String(name)).send().getValue();
        return dataUnionFromMainnetAddress(address);
    }


    public DataUnion dataUnionFromMainnetAddress(String mainnetAddress) throws Exception {
        DataUnionMainnet main =  mainnetDU(mainnetAddress);
        DataUnionSidechain side = sidechainDU(factoryMainnet().sidechainAddress(new Address(main.getContractAddress())).send().getValue());
        return new DataUnion(main, side);
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

    public ForeignAMB mainnetAMB(Credentials cred) throws Exception {
        String amb = factoryMainnet().amb().send().getValue();
        return ForeignAMB.load(amb, mainnet, cred, mainnetGasProvider());
    }

    public ForeignAMB mainnetAMB() throws Exception {
        return mainnetAMB(mainnetCred);
    }

    public HomeAMB sidechainAMB() throws Exception {
        String amb = factorySidechain().amb().send().getValue();
        return HomeAMB.load(amb, sidechain, sidechainCred, sidechainGasProvider());
    }


    /**
     *
     * @param msgHash
     * @param sleeptime
     * @param timeout
     * @return number of signatures if complete or null if incomplete
     * @throws Exception
     */
    public BigInteger waitForSidechainAMBAffirmations(final Bytes32 msgHash, long sleeptime, long timeout) throws Exception {
        Condition affirmationsChange = new Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger requiredSignatures = sidechainAMB().requiredSignatures().send().getValue();
                BigInteger signatures = sidechainAMB().numMessagesSigned(msgHash).send().getValue();
                //bit 255 is set in AMB to indicate completion. should the same as signatures >= reqd
                boolean complete = signatures.testBit(255);
                signatures = signatures.clearBit(255);
                if(!complete || signatures.compareTo(requiredSignatures) < 0)
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
    from foreign AMB hasEnoughValidSignatures(): Need to construct this binary blob:
    * @param _signatures bytes blob with signatures to be validated.
    * First byte X is a number of signatures in a blob,
    * next X bytes are v components of signatures,
    * next 32 * X bytes are r components of signatures,
    * next 32 * X bytes are s components of signatures.

    java byte is signed, so we use short to support 0-255

    should only be called on an AMB message that has required # signatures
    */
    public void portTxToMainnet(Bytes32 mhash, short collectedSignatures, Credentials cred) throws Exception {
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
        mainnetAMB(cred).executeSignatures(message, new DynamicBytes(signatures)).send();
    }

    public void portTxsToMainnet(TransactionReceipt withdraw, Credentials mainnetSender) throws Exception {
        List<Bytes32[]> msgs = extractAmbMessagesIdAndHash(withdraw);
        for(Bytes32[] msg : msgs){
            Bytes32 id = msg[0];
            Bytes32 hash = msg[1];
            //check if message has already been sent
            if(mainnetAMB().messageCallStatus(id).send().getValue() ||
                    !mainnetAMB().failedMessageSender(id).send().toUint().getValue().equals(BigInteger.ZERO)){
                log.warn("ForeignAMB has already seen msgId " + id + ". skipping");
                continue;
            }
            //wait until required signatures are present
            BigInteger signatures = waitForSidechainAMBAffirmations(hash, 10000, 600000);
            if(signatures == null){
                log.warn("Couldnt find affimation for AMB msgId " + id);
                continue;
            }
            log.info("Porting msgId " + id);
            portTxToMainnet(hash, signatures.shortValueExact(), mainnetSender);
        }
    }




    //utility functions:


    public static byte[] signWithdraw(Credentials account, byte[] req) throws Exception {
        Sign.SignatureData sig = Sign.signPrefixedMessage(req, account.getEcKeyPair());
        return toBytes65(sig);
    }

    /**
     *
     * @param tx
     * @return a list of [msgId, msgHash] for the AMB messages generated by sidechain withdrawal tx
     */

    public static List<Bytes32[]> extractAmbMessagesIdAndHash(TransactionReceipt tx){
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