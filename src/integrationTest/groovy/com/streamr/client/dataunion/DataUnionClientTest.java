package com.streamr.client.dataunion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.StreamrClient;
import com.streamr.client.StreamrConstant;
import com.streamr.client.dataunion.contracts.IERC20;
import com.streamr.client.options.EncryptionOptions;
import com.streamr.client.options.SigningOptions;
import com.streamr.client.options.StreamrClientOptions;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataUnionClientTest implements StreamrConstant {
  @BeforeAll
  public void setup() throws Exception {
    wallets = new Credentials[testrpc_keys.length];
    for (int i = 0; i < testrpc_keys.length; i++) {
      wallets[i] = Credentials.create(testrpc_keys[i]);
    }

    StreamrClientOptions opts =
        new StreamrClientOptions(
            null,
            SigningOptions.getDefault(),
            EncryptionOptions.getDefault(),
            StreamrConstant.DEFAULT_WEBSOCKET_URL,
            StreamrConstant.DEFAULT_REST_URL);
    opts.setSidechainRpcUrl(DEV_SIDECHAIN_RPC);
    opts.setMainnetRpcUrl(DEV_MAINCHAIN_RPC);
    opts.setDataUnionMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY);
    opts.setDataUnionSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY);
    streamrClient = new StreamrClient(opts);
    client = streamrClient.dataUnionClient(testrpc_keys[0], testrpc_keys[0]);
    du = client.dataUnionFromName(duname);
    //    public static IERC20 load(String contractAddress, Web3j web3j, Credentials credentials,
    // ContractGasProvider contractGasProvider)
    Web3j mainnet = Web3j.build(new HttpService(DEV_MAINCHAIN_RPC));
    mainnetToken =
        IERC20.load(
            client.mainnetTokenAddress(), mainnet, wallets[0], new EstimatedGasProvider(mainnet));
  }

  @AfterAll
  public void cleanup() {
    if (streamrClient != null) {
      streamrClient.disconnect();
    }
  }

  @Test
  void testDataUnion() throws Exception {
//  @Test
//  void createDU() throws Exception {
    du =
        client.deployDataUnion(
            duname,
            wallets[0].getAddress(),
            BigInteger.ZERO,
            Arrays.asList(wallets[0].getAddress()));
    assertTrue(du.waitForDeployment(10000, 600000));
    assertTrue(du.isDeployed());
    //    }
    //
    //    @Test
    //    void addMembers() throws Exception {
    EthereumTransactionReceipt tr;
    tr = du.addMembers(wallets[1].getAddress(), wallets[2].getAddress());

    client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000);
    assertEquals(du.activeMemberCount(), BigInteger.valueOf(2));
    assertTrue(du.isMemberActive(wallets[1].getAddress()));
    //    }
    //
    //    @Test
    //    public void testTransferAndSidechainStats() throws Exception {
    BigInteger sidechainEarnings = du.totalEarnings();
    Address address = new Address(du.getMainnetContractAddress());
    Uint256 amount = new Uint256(testSendAmount.multiply(BigInteger.valueOf(2)));
    final TransactionReceipt t = mainnetToken.transfer(address, amount).send();
    client.waitForMainnetTx(t.getTransactionHash(), 10000, 600000);
    du.sendTokensToBridge();
    assertNotNull(du.waitForEarningsChange(sidechainEarnings, 10000, 600000));
    assertEquals(du.getEarnings(wallets[1].getAddress()), testSendAmount);
    //    }
    //
    //    @Test
    //    public void withdrawMemberAsAdmin() throws Exception {
    String recipient = wallets[2].getAddress();
    BigInteger recipientBal = mainnetToken.balanceOf(new Address(recipient)).send().getValue();
    tr = du.withdrawAllTokensForSelfOrAsAdmin(recipient);
    client.portTxsToMainnet(tr, wallets[0].getEcKeyPair().getPrivateKey());

    assertTrue(client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000));
    assertEquals(
        client.waitForMainnetBalanceChange(recipientBal, recipient, 10000, 600000),
        recipientBal.add(testSendAmount));
    //    }
    //
    //    @Test
    //    public void signedWithdrawalForAnother() throws Exception {
    recipientBal = mainnetToken.balanceOf(new Address(recipient)).send().getValue();
    tr = du.withdrawAllTokensForMember(wallets[1].getEcKeyPair().getPrivateKey(), recipient);
    client.portTxsToMainnet(tr, wallets[0].getEcKeyPair().getPrivateKey());

    assertTrue(client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000));
    assertEquals(
        client.waitForMainnetBalanceChange(recipientBal, recipient, 10000, 600000),
        recipientBal.add(testSendAmount));
  }

  private StreamrClient streamrClient;
  private DataUnionClient client;
  private String[] testrpc_keys =
      new ArrayList<String>(
              Arrays.asList(
                  "0x5e98cce00cff5dea6b454889f359a4ec06b9fa6b88e9d69b86de8e1c81887da0",
                  "0xe5af7834455b7239881b85be89d905d6881dcb4751063897f12be1b0dd546bdb",
                  "0x4059de411f15511a85ce332e7a428f36492ab4e87c7830099dadbf130f1896ae",
                  "0x633a182fb8975f22aaad41e9008cb49a432e9fdfef37f151e9e7c54e96258ef9",
                  "0x957a8212980a9a39bf7c03dcbeea3c722d66f2b359c669feceb0e3ba8209a297",
                  "0xfe1d528b7e204a5bdfb7668a1ed3adfee45b4b96960a175c9ef0ad16dd58d728",
                  "0xd7609ae3a29375768fac8bc0f8c2f6ac81c5f2ffca2b981e6cf15460f01efe14",
                  "0xb1abdb742d3924a45b0a54f780f0f21b9d9283b231a0a0b35ce5e455fa5375e7",
                  "0x2cd9855d17e01ce041953829398af7e48b24ece04ff9d0e183414de54dc52285",
                  "0x2c326a4c139eced39709b235fffa1fde7c252f3f7b505103f7b251586c35d543"))
          .toArray(new String[0]);
  private BigInteger testSendAmount = BigInteger.valueOf(1000000000000000000l);
  private Credentials[] wallets;
  private DataUnion du;
  private IERC20 mainnetToken;
  private static final String duname = "test" + System.currentTimeMillis();
}
