package com.streamr.client.dataunion;

import com.streamr.client.dataunion.contracts.DataUnionMainnet;
import com.streamr.client.dataunion.contracts.DataUnionSidechain;
import com.streamr.client.options.DataUnionClientOptions;
import com.streamr.client.utils.Web3jUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import static com.streamr.client.utils.Web3jUtils.*;

public class DataUnion {
    private static final Logger log = LoggerFactory.getLogger(DataUnion.class);
    private static final BigInteger MAX_UINT256 = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE);

    public enum ActiveStatus{NONE, ACTIVE, INACTIVE};

    private final DataUnionMainnet mainnet;
    private final DataUnionSidechain sidechain;
    private final Web3j mainnetConnector;
    private final Web3j sidechainConnector;
    private final Credentials mainnetCred;
    private final Credentials sidechainCred;
    private final DataUnionClientOptions opts;

    protected static class RangeException extends Exception{
        public RangeException(String msg){
            super(msg);
        }
    }

    //use DataUnionClient to instantiate
    protected DataUnion(DataUnionMainnet mainnet, Web3j mainnetConnector, DataUnionSidechain sidechain, Web3j sidechainConnector, DataUnionClientOptions opts) {
        this.opts = opts;
        this.mainnet = mainnet;
        this.mainnetConnector = mainnetConnector;
        this.mainnetCred = Credentials.create(opts.getMainnetAdminPrvKey());
        this.sidechainCred = Credentials.create(opts.getSidechainAdminPrvKey());
        this.sidechain = sidechain;
        this.sidechainConnector = sidechainConnector;
    }

    public boolean waitForDeployment(long pollInterval, long timeout) throws Exception {
        String code = waitForCodeAtAddress(sidechain.getContractAddress(), sidechainConnector, pollInterval, timeout);
        return code != null && !code.equals("0x");
    }

    public String getMainnetContractAddress(){
        return mainnet.getContractAddress();
    }

    public String getSidechainContractAddress(){
        return sidechain.getContractAddress();
    }

    public boolean isDeployed() throws Exception {
        //this will check the condition only once:
        return waitForDeployment(0,0);
    }

   public EthereumTransactionReceipt refreshRevenue() throws Exception {
        return new EthereumTransactionReceipt(sidechain.refreshRevenue().send());
    }

    public EthereumTransactionReceipt sendTokensToBridge() throws Exception {
        return new EthereumTransactionReceipt(mainnet.sendTokensToBridge().send());
    }

    public EthereumTransactionReceipt setNewMemberEth(BigInteger amountWei) throws Exception {
        return new EthereumTransactionReceipt(sidechain.setNewMemberEth(new Uint256(amountWei)).send());
    }

    public BigInteger waitForEarningsChange(final BigInteger initialBalance, long pollInterval, long timeout) throws Exception {
        Web3jUtils.Condition earningsChange = new Web3jUtils.Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger bal = sidechain.totalEarnings().send().getValue();
                if(!bal.equals(initialBalance)) {
                    return bal;
                }
                return null;
            }
        };
        return (BigInteger) waitForCondition(earningsChange, pollInterval, timeout);
    }

    public EthereumTransactionReceipt addJoinPartAgents(String ... agents) throws Exception {
        return new EthereumTransactionReceipt(sidechain.addJoinPartAgents(asDynamicAddressArray(agents)).send());
    }

    public EthereumTransactionReceipt partMembers(String ... members) throws Exception {
        return new EthereumTransactionReceipt(sidechain.partMembers(asDynamicAddressArray(members)).send());
    }

    public EthereumTransactionReceipt addMembers(String ... members) throws Exception {
        return new EthereumTransactionReceipt(sidechain.addMembers(asDynamicAddressArray(members)).send());
    }

    protected void checkRange(BigInteger x, BigInteger min, BigInteger max) throws RangeException {
        if(x.compareTo(min) < 0 || x.compareTo(max) > 0){
            throw new RangeException("Amount must be between " + min + " and " + max);
        }
    }

    /**
     *
     * withdraw a member to his own address. Must be that member or admin.

     * @param member
     * @param amount amount in wei
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt withdrawTokensForSelfOrAsAdmin(String member, BigInteger amount, boolean sendWithdrawToMainnet) throws Exception {
        checkRange(amount, BigInteger.ONE, MAX_UINT256);
        return new EthereumTransactionReceipt(sidechain.withdraw(new Address(member), new Uint256(amount), new Bool(sendWithdrawToMainnet)).send());
    }
    public EthereumTransactionReceipt withdrawAllTokensForSelfOrAsAdmin(String member, boolean sendWithdrawToMainnet) throws Exception {
        return new EthereumTransactionReceipt(sidechain.withdrawAll(new Address(member), new Bool(sendWithdrawToMainnet)).send());
    }

    /**
     * sends TX to sidechain as admin. withdrawer doesnt pay TX fee
     *
     * @param withdrawerPrivateKey
     * @param to
     * @param amount amout in wei or 0 to withdraw everything
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt withdrawTokensForMember(BigInteger withdrawerPrivateKey, String to, BigInteger amount, boolean sendWithdrawToMainnet) throws Exception {
        return withdrawTokensForMember(Credentials.create(ECKeyPair.create(withdrawerPrivateKey)), to, amount, sendWithdrawToMainnet);
    }
    public EthereumTransactionReceipt withdrawAllTokensForMember(BigInteger withdrawerPrivateKey, String to, boolean sendWithdrawToMainnet) throws Exception {
        return withdrawTokensForMember(Credentials.create(ECKeyPair.create(withdrawerPrivateKey)), to, BigInteger.ZERO, sendWithdrawToMainnet);
    }

    /**
     * sends TX to sidechain as admin. withdrawer doesnt pay TX fee
     *
     * @param withdrawerPrivateKey
     * @param to
     * @param amount amout in wei or 0 to withdraw everything
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt withdrawTokensForMember(String withdrawerPrivateKey, String to, BigInteger amount, boolean sendWithdrawToMainnet) throws Exception {
        return withdrawTokensForMember(Credentials.create(withdrawerPrivateKey), to, amount, sendWithdrawToMainnet);
    }
    public EthereumTransactionReceipt withdrawAllTokensForMember(String withdrawerPrivateKey, String to, boolean sendWithdrawToMainnet) throws Exception {
        return withdrawTokensForMember(Credentials.create(withdrawerPrivateKey), to, BigInteger.ZERO, sendWithdrawToMainnet);
    }

    //amount == 0 means withdrawAll
    protected EthereumTransactionReceipt withdrawTokensForMember(Credentials member, String to, BigInteger amount, boolean sendWithdrawToMainnet) throws Exception {
        //0 is allowed in this protected method
        checkRange(amount, BigInteger.ZERO, MAX_UINT256);
        byte[] req = createWithdrawRequest(member.getAddress(), to, amount);
        byte[] sig = toBytes65(Sign.signPrefixedMessage(req, member.getEcKeyPair()));
        if(amount.equals(BigInteger.ZERO)){
            return withdrawAllTokensForMember(member.getAddress(), to, sig, sendWithdrawToMainnet);
        }
        else{
            return withdrawTokensForMember(member.getAddress(), to, sig, amount, sendWithdrawToMainnet);
        }
    }


    /**
     * sends TX to sidechain as admin. withdrawer doesnt pay TX fee
     * @param from
     * @param to
     * @param signedWithdrawalRequest
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt withdrawAllTokensForMember(String from, String to, byte[] signedWithdrawalRequest, boolean sendWithdrawToMainnet) throws Exception {
        return new EthereumTransactionReceipt(sidechain.withdrawAllToSigned(new Address(from), new Address(to), new Bool(sendWithdrawToMainnet), new DynamicBytes(signedWithdrawalRequest)).send());
    }

    /**
     * sends TX to sidechain as admin. withdrawer doesnt pay TX fee
     * @param from
     * @param to
     * @param signedWithdrawalRequest
     * @param amount
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt withdrawTokensForMember(String from, String to, byte[] signedWithdrawalRequest, BigInteger amount, boolean sendWithdrawToMainnet) throws Exception {
        return new EthereumTransactionReceipt(sidechain.withdrawToSigned(new Address(from), new Address(to), new Uint256(amount), new Bool(sendWithdrawToMainnet), new DynamicBytes(signedWithdrawalRequest)).send());
    }

    public BigInteger totalEarnings() throws Exception {
        return sidechain.totalEarnings().send().getValue();
    }

    public BigInteger totalEarningsWithdrawn() throws Exception {
        return sidechain.totalEarningsWithdrawn().send().getValue();
    }

    public BigInteger activeMemberCount() throws Exception {
        return sidechain.activeMemberCount().send().getValue();
    }

    public BigInteger inactiveMemberCount() throws Exception {
        return sidechain.inactiveMemberCount().send().getValue();
    }

    public BigInteger lifetimeMemberEarnings() throws Exception {
        return sidechain.lifetimeMemberEarnings().send().getValue();
    }

    public BigInteger joinPartAgentCount() throws Exception {
        return sidechain.joinPartAgentCount().send().getValue();
    }

    public BigInteger getEarnings(String member) throws Exception {
        return sidechain.getEarnings(new Address(member)).send().getValue();
    }

    public BigInteger getWithdrawn(String member) throws Exception {
        return sidechain.getWithdrawn(new Address(member)).send().getValue();
    }

    public BigInteger getWithdrawableEarnings(String member) throws Exception {
        return sidechain.getWithdrawableEarnings(new Address(member)).send().getValue();
    }

    public BigInteger getAdminFeeFraction() throws Exception {
        return mainnet.adminFeeFraction().send().getValue();
    }

    /**
     *
     * @param fractionInWei a fraction expressed in wei (ie 10^18 means 1)
     * @return
     * @throws Exception
     */
    public EthereumTransactionReceipt setAdminFeeFraction(BigInteger fractionInWei) throws Exception {
        checkRange(fractionInWei, BigInteger.ZERO, BigInteger.TEN.pow(18));
        return new EthereumTransactionReceipt(mainnet.setAdminFee(new Uint256(fractionInWei)).send());
    }

    public EthereumTransactionReceipt setAdminFeeFraction(double fraction) throws Exception {
        return setAdminFeeFraction(toWei(fraction));
    }



    //create unsigned blob. must be signed to submit
    protected byte[] createWithdrawAllRequest(String from, String to) throws Exception {
        return createWithdrawRequest(from, to, BigInteger.ZERO);
    }

    /**
     * creates the unsigned blob that must be signed to withdraw for another
     *
     * @param from
     * @param to
     * @param amount
     * @return
     * @throws Exception
     */
    protected byte[] createWithdrawRequest(String from, String to, BigInteger amount) throws Exception {
        Uint256 withdrawn = sidechain.getWithdrawn(new Address(from)).send();
        //TypeEncode doesnt expose a non-padding encode() :(
        String messageHex = TypeEncoder.encode(new Address(to)).substring(24) +
                TypeEncoder.encode(new Uint256(amount)) +
                TypeEncoder.encode(new Address(sidechain.getContractAddress())).substring(24) +
                TypeEncoder.encode(withdrawn);
        return Numeric.hexStringToByteArray(messageHex);
    }

    public boolean isMemberActive(String member) throws Exception {
        return ActiveStatus.ACTIVE.ordinal() == sidechain.memberData(new Address(member)).send().component1().getValue().longValue();
    }

    public boolean isMemberInactive(String member) throws Exception {
        return ActiveStatus.INACTIVE.ordinal() == sidechain.memberData(new Address(member)).send().component1().getValue().longValue();
    }

    public EthereumTransactionReceipt withdrawToBinance(BigInteger amount) throws Exception {
        return withdrawTokensForMember(sidechainCred, opts.getBinanceAdapterAddress(), amount, false);
    }

    public EthereumTransactionReceipt withdrawAllToBinance() throws Exception {
        return withdrawToBinance(BigInteger.ZERO);
    }

    /**
     * creates a signed withdrawal request to recipient using the sidechain private key
     * @param recipient
     * @return
     * @throws Exception
     */
    public String signWithdraw(String recipient, BigInteger amount) throws Exception {
        byte[] req = createWithdrawRequest(sidechainCred.getAddress(), recipient, amount);
        byte[] sig = toBytes65(Sign.signPrefixedMessage(req, sidechainCred.getEcKeyPair()));
        return Numeric.toHexString(sig);
    }

    public String signWithdrawAll(String recipient) throws Exception {
        return signWithdraw(recipient, BigInteger.ZERO);
    }

    public String signWithdrawAllToBinance() throws Exception {
        return signWithdrawAll(opts.getBinanceAdapterAddress());
    }

}
