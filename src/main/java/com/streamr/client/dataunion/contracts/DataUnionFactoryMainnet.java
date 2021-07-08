package com.streamr.client.dataunion.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.8.4.
 */
@SuppressWarnings("rawtypes")
public class DataUnionFactoryMainnet extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051610b05380380610b05833981810160405260a081101561003357600080fd5b508051602082015160408301516060840151608090940151600480546001600160a01b039586166001600160a01b0319918216179091556000805494861694821694909417909355600180549285169284169290921790915560028054939094169290911691909117909155600355610a54806100b16000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c806371f6d9651161006657806371f6d96514610117578063cb8a191b1461011f578063cfeef8071461025e578063d4c31bd414610266578063fc0c546a1461031c5761009e565b8063015388a1146100a35780630b23e95a146100c75780631062b39a146100e157806317c2a98c146100e9578063187ac4cb1461010f575b600080fd5b6100ab610324565b604080516001600160a01b039092168252519081900360200190f35b6100cf610333565b60408051918252519081900360200190f35b6100ab610339565b6100ab600480360360208110156100ff57600080fd5b50356001600160a01b0316610428565b6100ab610452565b6100ab610461565b6100ab6004803603608081101561013557600080fd5b6001600160a01b038235169160208101359181019060608101604082013564010000000081111561016557600080fd5b82018360208201111561017757600080fd5b8035906020019184602083028401116401000000008311171561019957600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156101e957600080fd5b8201836020820111156101fb57600080fd5b8035906020019184600183028401116401000000008311171561021d57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610470945050505050565b6100ab6106de565b6100ab6004803603604081101561027c57600080fd5b6001600160a01b0382351691908101906040810160208201356401000000008111156102a757600080fd5b8201836020820111156102b957600080fd5b803590602001918460018302840111640100000000831117156102db57600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295506106ed945050505050565b6100ab6107ae565b6002546001600160a01b031681565b60035481565b6000600460009054906101000a90046001600160a01b03166001600160a01b031663533426d16040518163ffffffff1660e01b815260040160206040518083038186803b15801561038957600080fd5b505afa15801561039d573d6000803e3d6000fd5b505050506040513d60208110156103b357600080fd5b50516040805163cd59658360e01b815290516001600160a01b039092169163cd59658391600480820192602092909190829003018186803b1580156103f757600080fd5b505afa15801561040b573d6000803e3d6000fd5b505050506040513d602081101561042157600080fd5b5051905090565b60015460025460009161044c916001600160a01b03918216919081169085166107fe565b92915050565b6004546001600160a01b031681565b6000546001600160a01b031681565b60008082336040516020018080602001836001600160a01b03166001600160a01b03168152602001828103825284818151815260200191508051906020019080838360005b838110156104cd5781810151838201526020016104b5565b50505050905090810190601f1680156104fa5780820380516001836020036101000a031916815260200191505b5093505050506040516020818303038152906040528051906020012090506060600460009054906101000a90046001600160a01b0316600260009054906101000a90046001600160a01b0316600354600160009054906101000a90046001600160a01b03168a8a8a60405160240180886001600160a01b03166001600160a01b03168152602001876001600160a01b03166001600160a01b03168152602001868152602001856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b0316815260200183815260200180602001828103825283818151815260200191508051906020019060200280838360005b838110156106115781810151838201526020016105f9565b50506040805193909501838103601f190184529094525060208101805163f658af4560e01b6001600160e01b0390911617905260008054919d509b506106769a5061066f99506001600160a01b0316975061086a9650505050505050565b83856108bc565b9050876001600160a01b031661068b82610428565b600054604080516001600160a01b039283168152905192821692918516917f7bb36c64b37ae129eda8a24fd78defec04cc7a06bb27863c5a4571dd5d70acee9181900360200190a4979650505050505050565b6001546001600160a01b031681565b604080516001600160a01b0384168183015260208082019283528351606083015283516000938493869388938392608001918601908083838a5b8381101561073f578181015183820152602001610727565b50505050905090810190601f16801561076c5780820380516001836020036101000a031916815260200191505b5060408051601f1981840301815291905280516020909101206000549095506107a694506001600160a01b031692503091508490506107fe565b949350505050565b6000600460009054906101000a90046001600160a01b03166001600160a01b031663836c081d6040518163ffffffff1660e01b815260040160206040518083038186803b1580156103f757600080fd5b60008061080a8561086a565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b604882015290565b825160009082816020870184f591506001600160a01b03821661091d576040805162461bcd60e51b8152602060048201526014602482015273195c9c9bdc97d85b1c9958591e50dc99585d195960621b604482015290519081900360640190fd5b835115610a16576000826001600160a01b0316856040518082805190602001908083835b602083106109605780518252601f199092019160209182019101610941565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d80600081146109c2576040519150601f19603f3d011682016040523d82523d6000602084013e6109c7565b606091505b5050905080610a14576040805162461bcd60e51b815260206004820152601460248201527332b93937b92fb4b734ba34b0b634bd30ba34b7b760611b604482015290519081900360640190fd5b505b50939250505056fea2646970667358221220702891fcf8408669cbdee25db30a65a167e3f515672e7418ff79ea04b806601064736f6c63430006060033";

    public static final String FUNC_DATAUNIONMAINNETTEMPLATE = "dataUnionMainnetTemplate";

    public static final String FUNC_DATAUNIONSIDECHAINFACTORY = "dataUnionSidechainFactory";

    public static final String FUNC_DATAUNIONSIDECHAINTEMPLATE = "dataUnionSidechainTemplate";

    public static final String FUNC_MIGRATIONMANAGER = "migrationManager";

    public static final String FUNC_SIDECHAINMAXGAS = "sidechainMaxGas";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_TOKEN = "token";

    public static final String FUNC_SIDECHAINADDRESS = "sidechainAddress";

    public static final String FUNC_MAINNETADDRESS = "mainnetAddress";

    public static final String FUNC_DEPLOYNEWDATAUNION = "deployNewDataUnion";

    public static final Event MAINNETDUCREATED_EVENT = new Event("MainnetDUCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>() {}));
    ;

    @Deprecated
    protected DataUnionFactoryMainnet(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DataUnionFactoryMainnet(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DataUnionFactoryMainnet(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DataUnionFactoryMainnet(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<MainnetDUCreatedEventResponse> getMainnetDUCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(MAINNETDUCREATED_EVENT, transactionReceipt);
        ArrayList<MainnetDUCreatedEventResponse> responses = new ArrayList<MainnetDUCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MainnetDUCreatedEventResponse typedResponse = new MainnetDUCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.mainnet = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.sidechain = (Address) eventValues.getIndexedValues().get(1);
            typedResponse.owner = (Address) eventValues.getIndexedValues().get(2);
            typedResponse.template = (Address) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<MainnetDUCreatedEventResponse> mainnetDUCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, MainnetDUCreatedEventResponse>() {
            @Override
            public MainnetDUCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(MAINNETDUCREATED_EVENT, log);
                MainnetDUCreatedEventResponse typedResponse = new MainnetDUCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.mainnet = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.sidechain = (Address) eventValues.getIndexedValues().get(1);
                typedResponse.owner = (Address) eventValues.getIndexedValues().get(2);
                typedResponse.template = (Address) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<MainnetDUCreatedEventResponse> mainnetDUCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MAINNETDUCREATED_EVENT));
        return mainnetDUCreatedEventFlowable(filter);
    }

    public RemoteFunctionCall<Address> dataUnionMainnetTemplate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATAUNIONMAINNETTEMPLATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> dataUnionSidechainFactory() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATAUNIONSIDECHAINFACTORY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> dataUnionSidechainTemplate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATAUNIONSIDECHAINTEMPLATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> migrationManager() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_MIGRATIONMANAGER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> sidechainMaxGas() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINMAXGAS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> amb() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AMB, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> token() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKEN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> sidechainAddress(Address mainetAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINADDRESS, 
                Arrays.<Type>asList(mainetAddress), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> mainnetAddress(Address deployer, Utf8String name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_MAINNETADDRESS, 
                Arrays.<Type>asList(deployer, name), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deployNewDataUnion(Address owner, Uint256 adminFeeFraction, DynamicArray<Address> agents, Utf8String name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DEPLOYNEWDATAUNION, 
                Arrays.<Type>asList(owner, adminFeeFraction, agents, name), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static DataUnionFactoryMainnet load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionFactoryMainnet(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DataUnionFactoryMainnet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionFactoryMainnet(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DataUnionFactoryMainnet load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DataUnionFactoryMainnet(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DataUnionFactoryMainnet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DataUnionFactoryMainnet(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, Address _migrationManager, Address _dataUnionMainnetTemplate, Address _dataUnionSidechainTemplate, Address _dataUnionSidechainFactory, Uint256 _sidechainMaxGas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionMainnetTemplate, _dataUnionSidechainTemplate, _dataUnionSidechainFactory, _sidechainMaxGas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, Address _migrationManager, Address _dataUnionMainnetTemplate, Address _dataUnionSidechainTemplate, Address _dataUnionSidechainFactory, Uint256 _sidechainMaxGas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionMainnetTemplate, _dataUnionSidechainTemplate, _dataUnionSidechainFactory, _sidechainMaxGas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, Address _migrationManager, Address _dataUnionMainnetTemplate, Address _dataUnionSidechainTemplate, Address _dataUnionSidechainFactory, Uint256 _sidechainMaxGas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionMainnetTemplate, _dataUnionSidechainTemplate, _dataUnionSidechainFactory, _sidechainMaxGas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, Address _migrationManager, Address _dataUnionMainnetTemplate, Address _dataUnionSidechainTemplate, Address _dataUnionSidechainFactory, Uint256 _sidechainMaxGas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionMainnetTemplate, _dataUnionSidechainTemplate, _dataUnionSidechainFactory, _sidechainMaxGas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class MainnetDUCreatedEventResponse extends BaseEventResponse {
        public Address mainnet;

        public Address sidechain;

        public Address owner;

        public Address template;
    }
}
