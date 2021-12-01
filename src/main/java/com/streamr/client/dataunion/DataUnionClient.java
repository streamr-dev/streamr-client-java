package com.streamr.client.dataunion;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.dataunion.contracts.*;
import com.streamr.client.options.DataUnionClientOptions;
import com.streamr.client.rest.SetBinanceRecipientFromSignature;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.utils.Web3jUtils;
import okhttp3.*;
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
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.streamr.client.utils.HttpUtils.MOSHI;
import static com.streamr.client.utils.Web3jUtils.*;
import static org.web3j.tx.Contract.staticExtractEventParameters;

import com.streamr.client.utils.Web3jUtils.Condition;
import org.web3j.utils.Numeric;


/**
 * client for DU2. functions include
 * 1. interact with DataUnionFactories: create DataUnion, load DataUnion.
 * 2. perform bridge functions: portTxsToMainnet
 * 3. util functions: waitForTx
 *
 * use DataUnion to perform functions on a particular DU.
 *
 */
public class DataUnionClient {
    private static final Logger log = LoggerFactory.getLogger(DataUnionClient.class);
    public static final JsonAdapter<SetBinanceRecipientFromSignature> setBinanceRecipientAdapter = MOSHI.adapter(SetBinanceRecipientFromSignature.class);

    private final Web3j mainnet;
    private final Web3j sidechain;
    private Web3j binance;
    private final Credentials mainnetCred;
    private final Credentials sidechainCred;
    private long bridgePollInterval = 10000;
    private long bridgePollTimeout = 600000;
    private EstimatedGasProvider mainnetGasProvider;
    private EstimatedGasProvider sidechainGasProvider;
    private EstimatedGasProvider binanceGasProvider;

    private final DataUnionClientOptions opts;

    public DataUnionClient(DataUnionClientOptions opts) {
        this.opts = opts;
        this.mainnet = Web3j.build(new HttpService(opts.getMainnetRPC()));
        this.mainnetCred = Credentials.create(opts.getMainnetAdminPrivateKey());
        this.sidechain = Web3j.build(new HttpService(opts.getSidechainRPC()));
        this.sidechainCred = Credentials.create(opts.getSidechainAdminPrivateKey());
        mainnetGasProvider = new EstimatedGasProvider(mainnet, 730000);
        sidechainGasProvider = new EstimatedGasProvider(sidechain, 3000000);
    }


    public void setMainnetMaxGasPrice(long maxPrice){
        mainnetGasProvider.setMaxGasPrice(maxPrice);
    }

    public void setSidechainMaxGasPrice(long maxPrice){
        sidechainGasProvider.setMaxGasPrice(maxPrice);
    }

    public void setMainnetMaxGas(BigInteger maxGas){
        mainnetGasProvider.setGasLimit(maxGas);
    }

    public void setSidechainMaxGas(BigInteger maxGas){
        sidechainGasProvider.setGasLimit(maxGas);
    }


    public long getBridgePollInterval(){
        return bridgePollInterval;
    }

    public long getBridgePollTimeout(){
        return bridgePollTimeout;
    }

    public void setBridgePollInterval(long bridgePollInterval){
        this.bridgePollInterval = bridgePollInterval;
    }

    public void setBridgePollTimeout(long bridgePollTimeout){
        this.bridgePollTimeout = bridgePollTimeout;
    }

    public DataUnion deployDataUnion(String name, String admin, double adminFeeFraction, List<String> agents) throws Exception {
        if(adminFeeFraction < 0 || adminFeeFraction > 1) {
            throw new NumberFormatException("adminFeeFraction must be between 0 and 1");
        }
        return deployDataUnion(name, admin, toWei(adminFeeFraction), agents);
    }

    public DataUnion deployDataUnion(String name, String admin, BigInteger adminFeeFractionWei, List<String> agents) throws Exception {
        ArrayList<Address> agentAddresses = new ArrayList<Address>(agents.size());
        for (String agent : agents) {
            agentAddresses.add(new Address(agent));
        }
        Utf8String duname = new Utf8String(name);
        factoryMainnet().deployNewDataUnion(
                new Address(admin),
                new Uint256(adminFeeFractionWei),
                new DynamicArray<Address>(Address.class, agentAddresses),
                duname
        ).send();
        Address mainnetAddress = factoryMainnet().mainnetAddress(new Address(mainnetCred.getAddress()), duname).send();
        Address sidechainAddress = factoryMainnet().sidechainAddress(mainnetAddress).send();
        return new DataUnion(mainnetDataUnion(mainnetAddress), mainnet, sidechainDataUnion(sidechainAddress), sidechain, opts);
    }

    /**
     * NOTE: this is VERY unsafe and should be removed! DUs previously deployed using a different factory will return wrong addresses
     * This should only be used during deployment to predict addresses.
     * @deprecated
     */
    public DataUnion dataUnionFromName(String name) throws Exception {
        Address duAddress = factoryMainnet().mainnetAddress(new Address(mainnetCred.getAddress()), new Utf8String(name)).send();
        return dataUnionFromMainnetAddress(duAddress.getValue());
    }

    public DataUnion dataUnionFromMainnetAddress(String mainnetAddress) throws Exception {
        DataUnionMainnet duMainnet = mainnetDataUnion(new Address(mainnetAddress));
        Address sidechainAddress = duMainnet.sidechainAddress().send();
        DataUnionSidechain duSidechain = sidechainDataUnion(sidechainAddress);
        return new DataUnion(duMainnet, mainnet, duSidechain, sidechain, opts);
    }

    protected OkHttpClient.Builder httpClientBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if(opts != null) {
            builder.connectTimeout(opts.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            builder.readTimeout(opts.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            builder.writeTimeout(opts.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
        return builder;
    }

    public void setBinanceRecipientViaWithdrawServer(String member, String binanceRecipient, String signature) throws IOException {
        HttpUrl.Builder builder = HttpUrl.parse(opts.getWithdrawServerBaseUrl()).newBuilder();
        builder.addPathSegment("binanceAdapterSetRecipient");
        HttpUrl url = builder.build();
        SetBinanceRecipientFromSignature set = new SetBinanceRecipientFromSignature(member, binanceRecipient, signature);
        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(HttpUtils.jsonType, setBinanceRecipientAdapter.toJson(set)));
        execute(reqBuilder.build(),null);
    }

    private <T> T execute(Request request, JsonAdapter<T> adapter) throws IOException {
        OkHttpClient client = new OkHttpClient(httpClientBuilder());
        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();
        try {
            HttpUtils.assertSuccessful(response);

            // Deserialize HTTP response to concrete type.
            return adapter == null ? null : adapter.fromJson(response.body().source());
        } finally {
            response.close();
        }
    }
            /*
*/

    protected BinanceAdapter binanceAdapterSidechain() {
        return BinanceAdapter.load(opts.getBinanceAdapterAddress(), sidechain, sidechainCred, sidechainGasProvider);
    }

    protected DataUnionFactoryMainnet factoryMainnet() {
        return DataUnionFactoryMainnet.load(opts.getDataUnionMainnetFactoryAddress(), mainnet, mainnetCred, mainnetGasProvider);
    }

    protected DataUnionMainnet mainnetDataUnion(Address contractAddress) {
        return DataUnionMainnet.load(contractAddress.getValue(), mainnet, mainnetCred, mainnetGasProvider);
    }

    protected DataUnionFactorySidechain factorySidechain() {
        return DataUnionFactorySidechain.load(opts.getDataUnionSidechainFactoryAddress(), sidechain, sidechainCred, sidechainGasProvider);
    }

    protected DataUnionSidechain sidechainDataUnion(Address contractAddress) {
        return DataUnionSidechain.load(contractAddress.getValue(), sidechain, sidechainCred, sidechainGasProvider);
    }

    public String mainnetTokenAddress() throws Exception {
        return  factoryMainnet().token().send().getValue();
    }

    public String sidechainTokenAddress() throws Exception {
        return  factorySidechain().token().send().getValue();
    }

    public String getBinanceDepositAddress(String user) throws Exception {
        BinanceAdapter ba = binanceAdapterSidechain();
        Address a = ba.binanceRecipient(new Address(user)).send().component1();
        if(a.toUint().getValue().equals(BigInteger.ZERO))
            return null;
        return a.getValue();
    }

    public EthereumTransactionReceipt setBinanceDepositAddress(String depositAddress) throws Exception {
        return new EthereumTransactionReceipt(binanceAdapterSidechain().setBinanceRecipient(new Address(depositAddress)).send());
    }

    /*
    lazy-instantiate binance Web3j connector
     */
    protected Web3j getBinanceWeb3j() {
        if(binance == null)
            binance = Web3j.build(new HttpService(opts.getBinanceRPC()));
        return binance;
    }

    //setBinanceDepositAddress

    /*
    bytes32 messageHash = keccak256(abi.encodePacked(
            "\x19Ethereum Signed Message:\n72", recipient, nonce, address(this)));
*/
    protected byte[] createSetBinanceRecipientRequest(String from, String recipient) throws Exception {
        BigInteger nonce = getBinanceSetRecipientNonce(from).add(BigInteger.ONE);
        //TypeEncode doesnt expose a non-padding encode() :(
        String messageHex = TypeEncoder.encode(new Address(recipient)).substring(24) +
                TypeEncoder.encode(new Uint256(nonce)) +
                TypeEncoder.encode(new Address(opts.getBinanceAdapterAddress())).substring(24);
        return Numeric.hexStringToByteArray(messageHex);
    }

    public String createSignedSetBinanceRecipientRequest(String recipient) throws Exception {
        return createSignedSetBinanceRecipientRequest(sidechainCred, recipient);
    }

    public String createSignedSetBinanceRecipientRequest(Credentials cred, String recipient) throws Exception {
        byte[] req = createSetBinanceRecipientRequest(cred.getAddress(), recipient);
        byte[] sig = toBytes65(Sign.signPrefixedMessage(req, cred.getEcKeyPair()));
        return Numeric.toHexString(sig);
    }

    protected BigInteger getBinanceSetRecipientNonce(String address) throws Exception {
        return binanceAdapterSidechain().binanceRecipient(new Address(address)).send().component2().getValue();
    }

    public EthereumTransactionReceipt setBinanceRecipientFromSig(String from, String recip, String signedSetBinanceRecipientRequest) throws Exception {
        byte[] sig = Numeric.hexStringToByteArray(signedSetBinanceRecipientRequest);
        return new EthereumTransactionReceipt(binanceAdapterSidechain().setBinanceRecipientFromSig(new Address(from), new Address(recip), new DynamicBytes(sig)).send());
    }

    public byte[] sign(byte[] request) {
        return toBytes65(Sign.signPrefixedMessage(request, sidechainCred.getEcKeyPair() ));
    }


    protected IERC20 mainnetToken() throws Exception {
        String tokenAddress = factoryMainnet().token().send().getValue();
        return IERC20.load(tokenAddress, mainnet, mainnetCred, mainnetGasProvider);
    }

    protected IERC20 sidechainToken() throws Exception {
        String tokenAddress = factorySidechain().token().send().getValue();
        return IERC20.load(tokenAddress, sidechain, sidechainCred, sidechainGasProvider);
    }

    protected ForeignAMB binanceAMB(Credentials cred) throws Exception {
        Web3j binance = getBinanceWeb3j();
        return ForeignAMB.load(opts.getBinanceSmartChainAMBAddress(), binance, cred, new EstimatedGasProvider(binance, 3000000));
    }

    protected ForeignAMB mainnetAMB(Credentials cred) throws Exception {
        String amb = factoryMainnet().amb().send().getValue();
        return ForeignAMB.load(amb, mainnet, cred, mainnetGasProvider);
    }

    protected ForeignAMB mainnetAMB() throws Exception {
        return mainnetAMB(mainnetCred);
    }

    protected HomeAMB sidechainAMBToMainnet() throws Exception {
        String amb = factorySidechain().amb().send().getValue();
        return HomeAMB.load(amb, sidechain, sidechainCred, sidechainGasProvider);
    }

    protected HomeAMB sidechainAMBToBinance() throws Exception {
        //pull the AMB address from the tokenMediator pointed to by Binance Adapter
        String tokenMediator = binanceAdapterSidechain().bscBridge().send().getValue();
        String amb = Web3jUtils.callAddressGetterFunction(tokenMediator, "bridgeContract", sidechain);
        return HomeAMB.load(amb, sidechain, sidechainCred, sidechainGasProvider);
    }

    /**
     *
     * waits until AMB message has been confirmed
     *
     * @param msgHash
     * @param sleeptime
     * @param timeout
     * @return number of signatures if complete or null if incomplete
     * @throws Exception
     */
    protected static BigInteger waitForAMBAffirmations(HomeAMB fromAMB, final Bytes32 msgHash, long sleeptime, long timeout) throws Exception {
        Condition affirmationsChange = new Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger requiredSignatures = fromAMB.requiredSignatures().send().getValue();
                BigInteger signatures = fromAMB.numMessagesSigned(msgHash).send().getValue();
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

    public EthereumTransactionReceipt setNewDUInitialEth(BigInteger amountWei) throws Exception {
        return new EthereumTransactionReceipt(factorySidechain().setNewDUInitialEth(new Uint256(amountWei)).send());
    }

    public EthereumTransactionReceipt setNewDUOwnerInitialEth(BigInteger amountWei) throws Exception {
        return new EthereumTransactionReceipt(factorySidechain().setNewDUOwnerInitialEth(new Uint256(amountWei)).send());
    }

    public EthereumTransactionReceipt setNewMemberInitialEth(BigInteger amountWei) throws Exception {
        return new EthereumTransactionReceipt(factorySidechain().setNewMemberInitialEth(new Uint256(amountWei)).send());
    }


    /**
     * port an AMB message that has required # signatures.
     *
     *
     * @param msgHash AMB message hash
     * @param collectedSignatures
     * @throws Exception
     *
     * from foreign AMB hasEnoughValidSignatures(): Need to construct this binary blob:
     * First byte X is a number of signatures in a blob,
     * next X bytes are v components of signatures,
     * next 32 * X bytes are r components of signatures,
     * next 32 * X bytes are s components of signatures.
    java byte is signed, so we use short to support 0-255

     */

    protected EthereumTransactionReceipt portTx(HomeAMB fromAMB, ForeignAMB toAMB, Bytes32 msgHash, short collectedSignatures) throws Exception {
        if(collectedSignatures > 255) {
            throw new UnsupportedOperationException("collectedSignatures cannot be greater than 255");
        }
        DynamicBytes message = fromAMB.message(msgHash).send();
        byte[] signatures = new byte[1 + (65*collectedSignatures)];
        signatures[0] = (byte) collectedSignatures;
        //collect signatures one by one from sidechain, add to blob
        for(short i = 0; i < collectedSignatures; i++){
            Sign.SignatureData sig = fromBytes65(fromAMB.signature(msgHash, new Uint256(i)).send().getValue());
            signatures[i+1] = sig.getV()[0];
            System.arraycopy(sig.getR(), 0, signatures, 1+collectedSignatures+(i*32), 32);
            System.arraycopy(sig.getS(), 0, signatures, 1+(collectedSignatures*33)+(i*32), 32);
        }
        return new EthereumTransactionReceipt(toAMB.executeSignatures(message, new DynamicBytes(signatures)).send());
    }

    public List<EthereumTransactionReceipt> portTxsToBinance(EthereumTransactionReceipt withdrawalTransaction, String binanceSenderPrivateKey) throws Exception {
        return portTxs(sidechainAMBToBinance(), binanceAMB(Credentials.create(binanceSenderPrivateKey)), withdrawalTransaction.tr);
    }

    public List<EthereumTransactionReceipt> portTxsToMainnet(EthereumTransactionReceipt withdrawalTransaction, String mainnetSenderPrivateKey) throws Exception {
        return portTxs(sidechainAMBToMainnet(), mainnetAMB(Credentials.create(mainnetSenderPrivateKey)), withdrawalTransaction.tr);
    }

    public List<EthereumTransactionReceipt> portTxsToMainnet(EthereumTransactionReceipt withdrawalTransaction, BigInteger mainnetSenderPrivateKey) throws Exception {
        return portTxs(sidechainAMBToMainnet(), mainnetAMB(Credentials.create(ECKeyPair.create(mainnetSenderPrivateKey))), withdrawalTransaction.tr);
    }

    /**
     * port all bridge requests triggered by sidechain withdraw transaction
     * @param withdrawTxHash
     * @param mainnetSenderPrivateKey
     * @return
     * @throws Exception
     */
    public List<EthereumTransactionReceipt> portTxsToMainnet(String withdrawTxHash, String mainnetSenderPrivateKey) throws Exception {
        Optional<TransactionReceipt> optional = sidechain.ethGetTransactionReceipt(withdrawTxHash).send().getTransactionReceipt();
        if(!optional.isPresent()) {
            throw new NoSuchElementException("No sidechain transaction found for txhash " + withdrawTxHash);
        }
        TransactionReceipt withdraw = optional.get();
        return portTxs(sidechainAMBToMainnet(), mainnetAMB(Credentials.create(mainnetSenderPrivateKey)), withdraw);
    }



    protected List<EthereumTransactionReceipt> portTxs(HomeAMB fromAMB, ForeignAMB toAMB, TransactionReceipt withdraw) throws Exception {
        List<Bytes32[]> msgs = extractAmbMessagesIdAndHash(withdraw);
        List<EthereumTransactionReceipt> txs = new ArrayList<EthereumTransactionReceipt>(msgs.size());
        for(Bytes32[] msg : msgs){
            Bytes32 id = msg[0];
            Bytes32 hash = msg[1];
            //check if message has already been sent
            if(toAMB.messageCallStatus(id).send().getValue() ||
                    !toAMB.failedMessageSender(id).send().toUint().getValue().equals(BigInteger.ZERO)){
                log.warn("ForeignAMB has already seen msgId " + Numeric.toHexString(id.getValue()) + ". skipping");
                continue;
            }
            //wait until required signatures are present
            BigInteger signatures = waitForAMBAffirmations(fromAMB, hash, bridgePollInterval, bridgePollTimeout);
            if(signatures == null){
                log.warn("Couldnt find affimation for AMB msgId " + id);
                continue;
            }
            log.info("Porting msgId " + id);
            txs.add(portTx(fromAMB, toAMB, hash, signatures.shortValueExact()));
        }
        return txs;
    }

    //utility functions:
    /**
     *
     * @param tx
     * @return a list of [msgId, msgHash] for the AMB messages generated by sidechain withdrawal tx
     */
    protected static List<Bytes32[]> extractAmbMessagesIdAndHash(TransactionReceipt tx){
        ArrayList<Bytes32[]> msgs = new ArrayList<Bytes32[]>();
        for(Log l : tx.getLogs()){
            EventValues vals = staticExtractEventParameters(HomeAMB.USERREQUESTFORSIGNATURE_EVENT, l);
            if(vals == null) {
                continue;
            }
            //event UserRequestForSignature(bytes32 indexed messageId, bytes encodedData);
            Bytes32[] idAndHash = new Bytes32[2];
            idAndHash[0] = (Bytes32) vals.getIndexedValues().get(0);
            idAndHash[1] = new Bytes32(Hash.sha3(((DynamicBytes) vals.getNonIndexedValues().get(0)).getValue()));
            msgs.add(idAndHash);
        }
        return msgs;
    }
}
