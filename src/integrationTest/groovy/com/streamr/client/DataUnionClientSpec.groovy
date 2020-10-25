package com.streamr.client

import com.streamr.client.dataunion.DataUnionClient
import com.streamr.client.dataunion.contracts.DataUnionFactoryMainnet
import com.streamr.client.dataunion.contracts.DataUnionFactorySidechain
import com.streamr.client.dataunion.contracts.DataUnionMainnet
import com.streamr.client.rest.FieldConfig
import com.streamr.client.rest.Stream
import com.streamr.client.rest.StreamConfig
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials

class DataUnionClientSpec extends StreamrIntegrationSpecification{
    private DataUnionClient client
    private String sidechainFactoryAddress = "0x4081B7e107E59af8E82756F96C751174590989FE"
    private String mainnetFactoryAddress = "0x5E959e5d5F3813bE5c6CeA996a286F734cc9593b"
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
    void setup() {
        client = devChainDataUnionClient()
    }


    /*
    void cleanup() {
        if (client != null) {
            client.disconnect()
        }
    }
    */

    void "create DU"() {
        Credentials key0 =  Credentials.create(testrpc_keys[0])
        DataUnionFactoryMainnet mainnetFactory = client.factoryMainnet(mainnetFactoryAddress, key0)
        DataUnionFactorySidechain sidechainFactory = client.factorySidechain(sidechainFactoryAddress, key0)
        Utf8String duname = new Utf8String("test"+System.currentTimeMillis())
        Address deployer = new Address(key0.getAddress())
        org.web3j.protocol.core.RemoteFunctionCall<Address> rfc = mainnetFactory.mainnetAddress(deployer, duname)
        Address duAddress = rfc.send()
        DataUnionMainnet duMainnet = client.mainnetDU(duAddress.getValue(), key0)
        System.out.println("duAddress = " + duAddress)
        when:
        mainnetFactory.deployNewDataUnion(
                deployer,
                new Uint256(123),
                new DynamicArray<Address>(deployer),
                duname
        ).send()

        then:
        duMainnet.token().send() != null
    }
}
