package com.streamr.client

import com.streamr.client.dataunion.DataUnion
import com.streamr.client.dataunion.DataUnionClient
import com.streamr.client.dataunion.EstimatedGasProvider
import com.streamr.client.dataunion.EthereumTransactionReceipt
import com.streamr.client.dataunion.contracts.IERC20
import com.streamr.client.options.EncryptionOptions
import com.streamr.client.options.SigningOptions
import com.streamr.client.options.StreamrClientOptions
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService

class DataUnionClientSpec extends StreamrIntegrationSpecification{
    private StreamrClient streamrClient
    private DataUnionClient client
    //truffle keys mnemonic "testrpc"
    private String[] testrpc_keys = [
        "0x5e98cce00cff5dea6b454889f359a4ec06b9fa6b88e9d69b86de8e1c81887da0",
        "0xe5af7834455b7239881b85be89d905d6881dcb4751063897f12be1b0dd546bdb",
        "0x4059de411f15511a85ce332e7a428f36492ab4e87c7830099dadbf130f1896ae",
        "0x633a182fb8975f22aaad41e9008cb49a432e9fdfef37f151e9e7c54e96258ef9",
        "0x957a8212980a9a39bf7c03dcbeea3c722d66f2b359c669feceb0e3ba8209a297",
        "0xfe1d528b7e204a5bdfb7668a1ed3adfee45b4b96960a175c9ef0ad16dd58d728",
        "0xd7609ae3a29375768fac8bc0f8c2f6ac81c5f2ffca2b981e6cf15460f01efe14",
        "0xb1abdb742d3924a45b0a54f780f0f21b9d9283b231a0a0b35ce5e455fa5375e7",
        "0x2cd9855d17e01ce041953829398af7e48b24ece04ff9d0e183414de54dc52285",
        "0x2c326a4c139eced39709b235fffa1fde7c252f3f7b505103f7b251586c35d543"
    ]
    private BigInteger testSendAmount = BigInteger.valueOf(1000000000000000000l)
    private Credentials[] wallets;
    private DataUnion du;
    private IERC20 mainnetToken;
    private static final String duname = "test"+System.currentTimeMillis();

    void setup() {
        wallets = new Credentials[testrpc_keys.length];
        for(int i=0; i < testrpc_keys.length; i++){
            wallets[i] = Credentials.create(testrpc_keys[i]);
        }
        StreamrClientOptions opts = new StreamrClientOptions(
                null,
                SigningOptions.getDefault(),
                EncryptionOptions.getDefault(),
                DEFAULT_WEBSOCKET_URL,
                DEFAULT_REST_URL
        )
        opts.setSidechainRpcUrl(DEV_SIDECHAIN_RPC)
        opts.setMainnetRpcUrl(DEV_MAINCHAIN_RPC)
        opts.setDataUnionMainnetFactoryAddress(DEV_MAINCHAIN_FACTORY)
        opts.setDataUnionSidechainFactoryAddress(DEV_SIDECHAIN_FACTORY)
        streamrClient = new StreamrClient(opts)
        client = streamrClient.dataUnionClient(testrpc_keys[0], testrpc_keys[0])
        du = client.dataUnionFromName(duname);
        //    public static IERC20 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider)
        Web3j mainnet = Web3j.build(new HttpService(DEV_MAINCHAIN_RPC))
        mainnetToken = IERC20.load(client.mainnetTokenAddress(), mainnet, wallets[0], new EstimatedGasProvider(mainnet))
    }

    void cleanup() {
        if (streamrClient != null) {
            streamrClient.disconnect()
        }
    }

    void "create DU"() {
        //Address deployer = new Address(wallets[0].getAddress())
        when:
        du = client.deployDataUnion(
                duname,
                wallets[0].getAddress(),
                BigInteger.ZERO,
                Arrays.<String>asList(wallets[0].getAddress()),
        )

        then:
        du.waitForDeployment(10000, 600000)
        du.isDeployed()
    }

    void "add members"() {
        EthereumTransactionReceipt tr;
        when:
        tr = du.addMembers(wallets[1].getAddress(), wallets[2].getAddress())

        then:
        client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000)
        du.activeMemberCount().equals(BigInteger.valueOf(2))
        du.isMemberActive(wallets[1].getAddress())
    }

    void "test transfer and sidechain stats"() {
        BigInteger sidechainEarnings = du.totalEarnings()
        when:
        TransactionReceipt tr = mainnetToken.transfer(new Address(du.getMainnetContractAddress()), new Uint256(testSendAmount.multiply(BigInteger.valueOf(2)))).send()
        client.waitForMainnetTx(tr.getTransactionHash(), 10000, 600000)
        du.sendTokensToBridge();
        then:
        du.waitForEarningsChange(sidechainEarnings, 10000, 600000) != null
        du.getEarnings(wallets[1].getAddress()).equals(testSendAmount)
    }

    void "withdraw member as admin"() {
        String recipient = wallets[2].getAddress();
        BigInteger recipientBal = mainnetToken.balanceOf(new Address(recipient)).send().getValue();
        EthereumTransactionReceipt tr;
        when:
        tr = du.withdrawAllTokensForSelfOrAsAdmin(recipient)
        client.portTxsToMainnet(tr, wallets[0].getEcKeyPair().getPrivateKey())

        then:
        client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000)
        client.waitForMainnetBalanceChange(recipientBal,recipient, 10000, 600000 ) == recipientBal.add(testSendAmount)
    }

    void "signed withdrawal for another"() {
        String recipient = wallets[2].getAddress();
        BigInteger recipientBal = mainnetToken.balanceOf(new Address(recipient)).send().getValue();
        EthereumTransactionReceipt tr;
        when:
        tr = du.withdrawAllTokensForMember(wallets[1].getEcKeyPair().getPrivateKey(), recipient)
        client.portTxsToMainnet(tr, wallets[0].getEcKeyPair().getPrivateKey())

        then:
        client.waitForSidechainTx(tr.getTransactionHash(), 10000, 600000)
        client.waitForMainnetBalanceChange(recipientBal,recipient, 10000, 600000 ) == recipientBal.add(testSendAmount)
    }
}
