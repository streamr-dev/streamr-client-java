package com.streamr.client.dataunion;

import static com.streamr.client.dataunion.Web3jUtils.asDynamicAddressArray;
import static com.streamr.client.dataunion.Web3jUtils.toBytes65;
import static com.streamr.client.dataunion.Web3jUtils.waitForCodeAtAddress;
import static com.streamr.client.dataunion.Web3jUtils.waitForCondition;

import com.streamr.client.dataunion.contracts.DataUnionMainnet;
import com.streamr.client.dataunion.contracts.DataUnionSidechain;
import java.math.BigInteger;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

public final class DataUnion {
  private static final BigInteger MAX_UINT256 =
      BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE);

  public enum ActiveStatus {
    NONE,
    ACTIVE,
    INACTIVE
  };

  private final DataUnionMainnet mainnet;
  private final DataUnionSidechain sidechain;
  private final Web3j mainnetConnector;
  private final Web3j sidechainConnector;
  private final Credentials mainnetCred;
  private final Credentials sidechainCred;

  protected static class RangeException extends Exception {
    public RangeException(final String msg) {
      super(msg);
    }
  }

  // use DataUnionClient to instantiate
  protected DataUnion(
      final DataUnionMainnet mainnet,
      final Web3j mainnetConnector,
      final Credentials mainnetCred,
      final DataUnionSidechain sidechain,
      final Web3j sidechainConnector,
      final Credentials sidechainCred) {
    this.mainnet = mainnet;
    this.mainnetConnector = mainnetConnector;
    this.mainnetCred = mainnetCred;
    this.sidechain = sidechain;
    this.sidechainConnector = sidechainConnector;
    this.sidechainCred = sidechainCred;
  }

  public boolean waitForDeployment(final long pollInterval, final long timeout) throws Exception {
    String code =
        waitForCodeAtAddress(
            sidechain.getContractAddress(), sidechainConnector, pollInterval, timeout);
    return code != null && !code.equals("0x");
  }

  public String getMainnetContractAddress() {
    return mainnet.getContractAddress();
  }

  public String getSidechainContractAddress() {
    return sidechain.getContractAddress();
  }

  public boolean isDeployed() throws Exception {
    // this will check the condition only once:
    return waitForDeployment(0, 0);
  }

  public void refreshRevenue() throws Exception {
    sidechain.refreshRevenue().send();
  }

  public void sendTokensToBridge() throws Exception {
    mainnet.sendTokensToBridge().send();
  }

  public void setNewMemberEth(final BigInteger amountWei) throws Exception {
    sidechain.setNewMemberEth(new Uint256(amountWei)).send();
  }

  public BigInteger waitForEarningsChange(
      final BigInteger initialBalance, final long pollInterval, final long timeout)
      throws Exception {
    Web3jUtils.Condition earningsChange =
        new Web3jUtils.Condition() {
          @Override
          public Object check() throws Exception {
            BigInteger bal = sidechain.totalEarnings().send().getValue();
            if (!bal.equals(initialBalance)) {
              return bal;
            }
            return null;
          }
        };
    return (BigInteger) waitForCondition(earningsChange, pollInterval, timeout);
  }

  public EthereumTransactionReceipt addJoinPartAgents(final String... agents) throws Exception {
    return new EthereumTransactionReceipt(
        sidechain.addJoinPartAgents(asDynamicAddressArray(agents)).send());
  }

  public EthereumTransactionReceipt partMembers(final String... members) throws Exception {
    return new EthereumTransactionReceipt(
        sidechain.partMembers(asDynamicAddressArray(members)).send());
  }

  public EthereumTransactionReceipt addMembers(final String... members) throws Exception {
    return new EthereumTransactionReceipt(
        sidechain.addMembers(asDynamicAddressArray(members)).send());
  }

  protected void checkRange(final BigInteger x, final BigInteger min, final BigInteger max)
      throws RangeException {
    if (x.compareTo(min) < 0 || x.compareTo(max) > 0) {
      throw new RangeException("Amount must be between " + min + " and " + max);
    }
  }

  /**
   * Withdraw a member to his own address. Must be that member or admin.
   *
   * @param amount amount in wei
   */
  public EthereumTransactionReceipt withdrawTokensForSelfOrAsAdmin(
      final String member, final BigInteger amount) throws Exception {
    checkRange(amount, BigInteger.ONE, MAX_UINT256);
    return new EthereumTransactionReceipt(
        sidechain.withdraw(new Address(member), new Uint256(amount), new Bool(true)).send());
  }

  public EthereumTransactionReceipt withdrawAllTokensForSelfOrAsAdmin(final String member)
      throws Exception {
    return new EthereumTransactionReceipt(
        sidechain.withdrawAll(new Address(member), new Bool(true)).send());
  }

  /**
   * Sends TX to sidechain as admin. withdrawer doesnt pay TX fee
   *
   * @param amount amout in wei or 0 to withdraw everything
   */
  public EthereumTransactionReceipt withdrawTokensForMember(
      final BigInteger withdrawerPrivateKey, final String to, final BigInteger amount)
      throws Exception {
    return withdrawTokensForMember(
        Credentials.create(ECKeyPair.create(withdrawerPrivateKey)), to, amount);
  }

  public EthereumTransactionReceipt withdrawAllTokensForMember(
      final BigInteger withdrawerPrivateKey, final String to) throws Exception {
    return withdrawTokensForMember(
        Credentials.create(ECKeyPair.create(withdrawerPrivateKey)), to, BigInteger.ZERO);
  }

  /**
   * sends TX to sidechain as admin. withdrawer doesnt pay TX fee
   *
   * @param amount amout in wei or 0 to withdraw everything
   */
  public EthereumTransactionReceipt withdrawTokensForMember(
      final String withdrawerPrivateKey, final String to, final BigInteger amount)
      throws Exception {
    return withdrawTokensForMember(Credentials.create(withdrawerPrivateKey), to, amount);
  }

  public EthereumTransactionReceipt withdrawAllTokensForMember(
      final String withdrawerPrivateKey, final String to) throws Exception {
    return withdrawTokensForMember(Credentials.create(withdrawerPrivateKey), to, BigInteger.ZERO);
  }

  // amount == 0 means withdrawAll
  protected EthereumTransactionReceipt withdrawTokensForMember(
      final Credentials member, final String to, final BigInteger amount) throws Exception {
    // 0 is allowed in this protected method
    checkRange(amount, BigInteger.ZERO, MAX_UINT256);
    byte[] req = createWithdrawRequest(member.getAddress(), to, amount);
    byte[] sig = toBytes65(Sign.signPrefixedMessage(req, member.getEcKeyPair()));
    if (amount.equals(BigInteger.ZERO)) {
      return withdrawAllTokensForMember(member.getAddress(), to, sig);
    } else {
      return withdrawTokensForMember(member.getAddress(), to, sig, amount);
    }
  }

  /** Sends TX to sidechain as admin. withdrawer doesnt pay TX fee */
  public EthereumTransactionReceipt withdrawAllTokensForMember(
      final String from, final String to, final byte[] signedWithdrawalRequest) throws Exception {
    return new EthereumTransactionReceipt(
        sidechain
            .withdrawAllToSigned(
                new Address(from),
                new Address(to),
                new Bool(true),
                new DynamicBytes(signedWithdrawalRequest))
            .send());
  }

  /** Sends TX to sidechain as admin. withdrawer doesnt pay TX fee */
  public EthereumTransactionReceipt withdrawTokensForMember(
      final String from,
      final String to,
      final byte[] signedWithdrawalRequest,
      final BigInteger amount)
      throws Exception {
    return new EthereumTransactionReceipt(
        sidechain
            .withdrawToSigned(
                new Address(from),
                new Address(to),
                new Uint256(amount),
                new Bool(true),
                new DynamicBytes(signedWithdrawalRequest))
            .send());
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

  public BigInteger getEarnings(final String member) throws Exception {
    return sidechain.getEarnings(new Address(member)).send().getValue();
  }

  public BigInteger getWithdrawn(final String member) throws Exception {
    return sidechain.getWithdrawn(new Address(member)).send().getValue();
  }

  public BigInteger getWithdrawableEarnings(final String member) throws Exception {
    return sidechain.getWithdrawableEarnings(new Address(member)).send().getValue();
  }

  // create unsigned blob. must be signed to submit
  protected byte[] createWithdrawAllRequest(final String from, final String to) throws Exception {
    return createWithdrawRequest(from, to, BigInteger.ZERO);
  }

  protected byte[] createWithdrawRequest(
      final String from, final String to, final BigInteger amount) throws Exception {
    Uint256 withdrawn = sidechain.getWithdrawn(new Address(from)).send();
    // TypeEncode doesnt expose a non-padding encode() :(
    String messageHex =
        TypeEncoder.encode(new Address(to)).substring(24)
            + TypeEncoder.encode(new Uint256(amount))
            + TypeEncoder.encode(new Address(sidechain.getContractAddress())).substring(24)
            + TypeEncoder.encode(withdrawn);
    return Numeric.hexStringToByteArray(messageHex);
  }

  public boolean isMemberActive(final String member) throws Exception {
    return ActiveStatus.ACTIVE.ordinal()
        == sidechain.memberData(new Address(member)).send().component1().getValue().longValue();
  }

  public boolean isMemberInactive(final String member) throws Exception {
    return ActiveStatus.INACTIVE.ordinal()
        == sidechain.memberData(new Address(member)).send().component1().getValue().longValue();
  }
}
