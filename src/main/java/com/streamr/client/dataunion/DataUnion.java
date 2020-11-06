package com.streamr.client.dataunion;

import com.streamr.client.dataunion.contracts.DataUnionMainnet;
import com.streamr.client.dataunion.contracts.DataUnionSidechain;
import com.streamr.client.dataunion.contracts.IERC20;
import com.streamr.client.utils.Web3jUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;

import static com.streamr.client.dataunion.DataUnionClient.signWithdraw;
import static com.streamr.client.utils.Web3jUtils.waitForCodeAtAddress;
import static com.streamr.client.utils.Web3jUtils.waitForCondition;

public class DataUnion {
    private static final Logger log = LoggerFactory.getLogger(DataUnion.class);
    protected static final long STATUS_NONE = 0;
    protected static final long STATUS_ACTIVE = 1;
    protected static final long STATUS_INACTIVE = 2;

    // Contract.web3j is protected, so need to access with reflection. more bad web3j design!
    private static Web3j getWeb3j(Contract c) {
        try {
            return (Web3j) FieldUtils.readField(c, "web3j", true);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private DataUnionMainnet mainnet;
    private DataUnionSidechain sidechain;

    //use DataUnionClient to instantiate
    protected DataUnion(DataUnionMainnet mainnet, DataUnionSidechain sidechain) {
        this.mainnet = mainnet;
        this.sidechain = sidechain;
    }

    public boolean waitForDeployment(long sleeptime, long timeout) throws Exception {
        String code = waitForCodeAtAddress(sidechain.getContractAddress(), getWeb3j(sidechain), sleeptime, timeout);
        return code != null && !code.equals("0x");
    }

    public String mainnetAddress(){
        return mainnet.getContractAddress();
    }

    public String sidechainAddress(){
        return sidechain.getContractAddress();
    }

    public boolean isDeployed() throws Exception {
        //this will check the condition only once:
        return waitForDeployment(0,0);
    }

    public void sendTokensToBridge() throws Exception {
        mainnet.sendTokensToBridge().send();
    }

    public BigInteger waitForEarningsChange(final BigInteger initialBalance, long sleeptime, long timeout) throws Exception {
        Web3jUtils.Condition earningsChange = new Web3jUtils.Condition(){
            @Override
            public Object check() throws Exception {
                BigInteger bal = sidechain.totalEarnings().send().getValue();
                if(!bal.equals(initialBalance))
                    return bal;
                return null;
            }
        };
        return (BigInteger) waitForCondition(earningsChange, sleeptime, timeout);
   }

    public EthereumTransactionReceipt joinMembers(String ... members) throws Exception {
        ArrayList<Address> memberAddresses = new ArrayList<Address>(members.length);
        for (String member : members) {
            memberAddresses.add(new Address(member));
        }
        return new EthereumTransactionReceipt(sidechain.addMembers(new DynamicArray<Address>(Address.class, memberAddresses)).send());
    }

    //amount == 0 means withdrawAll!
    public EthereumTransactionReceipt withdraw(BigInteger withdrawerPrivateKey, String to, BigInteger amount) throws Exception {
        return withdraw(Credentials.create(ECKeyPair.create(withdrawerPrivateKey)), to, amount);
    }

    public EthereumTransactionReceipt withdraw(String withdrawerPrivateKey, String to, BigInteger amount) throws Exception {
        return withdraw(Credentials.create(withdrawerPrivateKey), to, amount);
    }

    protected EthereumTransactionReceipt withdraw(Credentials from, String to, BigInteger amount) throws Exception {
        byte[] req = createWithdrawRequest(from.getAddress(), to, amount);
        byte[] sig = signWithdraw(from, req);
        if(amount.equals(BigInteger.ZERO))
            return new EthereumTransactionReceipt(sidechain.withdrawAllToSigned(new Address(from.getAddress()), new Address(to), new Bool(true), new DynamicBytes(sig)).send());
        else
            return new EthereumTransactionReceipt(sidechain.withdrawToSigned(new Address(from.getAddress()), new Address(to), new Uint256(amount), new Bool(true), new DynamicBytes(sig)).send());
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

    //create unsigned blob. must be signed to submit
    protected byte[] createWithdrawAllRequest(String from, String to) throws Exception {
        return createWithdrawRequest(from, to, BigInteger.ZERO);
    }

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
        return STATUS_ACTIVE == sidechain.memberData(new Address(member)).send().component1().getValue().longValue();
    }
}
