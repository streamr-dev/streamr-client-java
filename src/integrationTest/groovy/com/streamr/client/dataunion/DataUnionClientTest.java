package com.streamr.client.dataunion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.StreamrClient;
import com.streamr.client.dataunion.contracts.IERC20;
import com.streamr.client.options.EncryptionOptions;
import com.streamr.client.options.SigningOptions;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.testing.TestingMeta;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

@Timeout(value = 11, unit = TimeUnit.MINUTES)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataUnionClientTest {
  static final String DEV_MAINCHAIN_RPC = "http://localhost:8545";
  static final String DEV_SIDECHAIN_RPC = "http://localhost:8546";
  static final String DEV_SIDECHAIN_FACTORY = "0x4081B7e107E59af8E82756F96C751174590989FE";
  static final String DEV_MAINCHAIN_FACTORY = "0x5E959e5d5F3813bE5c6CeA996a286F734cc9593b";
  static final String[] TEST_RPC_KEYS =
      new String[] {
        "0x5e98cce00cff5dea6b454889f359a4ec06b9fa6b88e9d69b86de8e1c81887da0",
        "0xe5af7834455b7239881b85be89d905d6881dcb4751063897f12be1b0dd546bdb",
        "0x4059de411f15511a85ce332e7a428f36492ab4e87c7830099dadbf130f1896ae",
        /*
        "0x633a182fb8975f22aaad41e9008cb49a432e9fdfef37f151e9e7c54e96258ef9",
        "0x957a8212980a9a39bf7c03dcbeea3c722d66f2b359c669feceb0e3ba8209a297",
        "0xfe1d528b7e204a5bdfb7668a1ed3adfee45b4b96960a175c9ef0ad16dd58d728",
        "0xd7609ae3a29375768fac8bc0f8c2f6ac81c5f2ffca2b981e6cf15460f01efe14",
        "0xb1abdb742d3924a45b0a54f780f0f21b9d9283b231a0a0b35ce5e455fa5375e7",
        "0x2cd9855d17e01ce041953829398af7e48b24ece04ff9d0e183414de54dc52285",
        "0x2c326a4c139eced39709b235fffa1fde7c252f3f7b505103f7b251586c35d543",
        */
      };
  static final String DATA_UNION_NAME = "test" + System.currentTimeMillis();

  StreamrClient streamrClient;
  DataUnionClient client;
  Credentials adminWallet;
  Credentials member1Wallet;
  Credentials member2Wallet;
  DataUnion du;
  IERC20 mainnetToken;

  @BeforeAll
  void setup() throws Exception {
    adminWallet = Credentials.create(TEST_RPC_KEYS[0]);
    member1Wallet = Credentials.create(TEST_RPC_KEYS[1]);
    member2Wallet = Credentials.create(TEST_RPC_KEYS[2]);

    StreamrClientOptions opts =
        new StreamrClientOptions(
            null,
            SigningOptions.getDefault(),
            EncryptionOptions.getDefault(),
            TestingMeta.WEBSOCKET_URL,
            TestingMeta.REST_URL);
    opts.setSidechainRpcUrl(DEV_SIDECHAIN_RPC);
    opts.setMainnetRpcUrl(DEV_MAINCHAIN_RPC);
    opts.setDataUnionMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY);
    opts.setDataUnionSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY);
    streamrClient = new StreamrClient(opts);
    final String adminPk = TEST_RPC_KEYS[0];
    client = streamrClient.dataUnionClient(adminPk, adminPk);
    du = client.dataUnionFromName(DATA_UNION_NAME);
    // public static IERC20 load(String contractAddress, Web3j web3j, Credentials credentials,
    // ContractGasProvider contractGasProvider)
    Web3j mainnet = Web3j.build(new HttpService(DEV_MAINCHAIN_RPC));
    mainnetToken =
        IERC20.load(
            client.mainnetTokenAddress(), mainnet, adminWallet, new EstimatedGasProvider(mainnet));
  }

  @AfterAll
  void cleanup() {
    if (streamrClient != null) {
      streamrClient.disconnect();
    }
  }

  final long pollInterval = 10000;
  final long timeout = 600000;

  @Test
  @Order(1)
  void createDataUnion() throws Exception {
    du =
        client.deployDataUnion(
            DATA_UNION_NAME,
            adminWallet.getAddress(),
            BigInteger.ZERO,
            Arrays.asList(adminWallet.getAddress()));

    assertTrue(du.waitForDeployment(pollInterval, timeout));
    assertTrue(du.isDeployed());
  }

  @Test
  @Order(2)
  void addMembersToDataUnion() throws Exception {
    final EthereumTransactionReceipt tr =
        du.addMembers(member1Wallet.getAddress(), member2Wallet.getAddress());

    client.waitForSidechainTx(tr.getTransactionHash(), pollInterval, timeout);

    assertEquals(du.activeMemberCount(), BigInteger.valueOf(2));
    assertTrue(du.isMemberActive(member1Wallet.getAddress()));
  }

  final BigInteger testSendAmount = BigInteger.valueOf(1000000000000000000l);

  @Test
  @Order(3)
  void testTransferAndSidechainStats() throws Exception {
    final BigInteger sidechainEarnings = du.totalEarnings();
    final Address address = new Address(du.getMainnetContractAddress());
    final Uint256 amount = new Uint256(testSendAmount.multiply(BigInteger.valueOf(2)));
    final TransactionReceipt t = mainnetToken.transfer(address, amount).send();

    client.waitForMainnetTx(t.getTransactionHash(), pollInterval, timeout);
    du.sendTokensToBridge();

    assertNotNull(du.waitForEarningsChange(sidechainEarnings, pollInterval, timeout));
    assertEquals(du.getEarnings(member1Wallet.getAddress()), testSendAmount);
  }

  @Test
  @Order(4)
  void withdrawMemberAsAdmin() throws Exception {
    final String recipient = member2Wallet.getAddress();
    final BigInteger recipientBal =
        mainnetToken.balanceOf(new Address(recipient)).send().getValue();
    final EthereumTransactionReceipt tr = du.withdrawAllTokensForSelfOrAsAdmin(recipient);

    client.portTxsToMainnet(tr, adminWallet.getEcKeyPair().getPrivateKey());

    assertTrue(client.waitForSidechainTx(tr.getTransactionHash(), pollInterval, timeout));
    assertEquals(
        client.waitForMainnetBalanceChange(recipientBal, recipient, pollInterval, timeout),
        recipientBal.add(testSendAmount));
  }

  @Test
  @Order(5)
  void signedWithdrawalForAnother() throws Exception {
    final Address member1Address = new Address(member1Wallet.getAddress());
    final BigInteger recipientBal = mainnetToken.balanceOf(member1Address).send().getValue();
    final EthereumTransactionReceipt tr =
        du.withdrawAllTokensForMember(
            member1Wallet.getEcKeyPair().getPrivateKey(), member1Wallet.getAddress());

    client.portTxsToMainnet(tr, adminWallet.getEcKeyPair().getPrivateKey());

    assertTrue(client.waitForSidechainTx(tr.getTransactionHash(), pollInterval, timeout));
    assertEquals(
        client.waitForMainnetBalanceChange(
            recipientBal, member1Wallet.getAddress(), pollInterval, timeout),
        recipientBal.add(testSendAmount));
  }
}
