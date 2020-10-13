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
 * <p>Generated with web3j version 5.0.0.
 */
@SuppressWarnings("rawtypes")
public class DataUnionFactoryMainnet extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051610a46380380610a46833981810160405260a081101561003357600080fd5b5080516020808301516040808501516060860151608090960151600580546001600160a01b03199081166001600160a01b03808a16919091179283905560008054831682891617905560018054831682871617905560028054909216818b1617909155845163cd59658360e01b81529451979895979396929491169263cd59658392600483810193919291829003018186803b1580156100d257600080fd5b505afa1580156100e6573d6000803e3d6000fd5b505050506040513d60208110156100fc57600080fd5b5051600480546001600160a01b039092166001600160a01b031990921691909117905560035550505050610911806101356000396000f3fe608060405234801561001057600080fd5b50600436106100935760003560e01c8063692199d411610066578063692199d4146100f2578063c5a8c91f1461010c578063cb8a191b14610114578063d4c31bd414610253578063e4a154a41461030957610093565b8063015a0da0146100985780631062b39a146100bc57806317c2a98c146100c457806335813bc1146100ea575b600080fd5b6100a0610311565b604080516001600160a01b039092168252519081900360200190f35b6100a0610320565b6100a0600480360360208110156100da57600080fd5b50356001600160a01b031661032f565b6100a0610359565b6100fa610368565b60408051918252519081900360200190f35b6100a061036e565b6100a06004803603608081101561012a57600080fd5b6001600160a01b038235169160208101359181019060608101604082013564010000000081111561015a57600080fd5b82018360208201111561016c57600080fd5b8035906020019184602083028401116401000000008311171561018e57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092959493602081019350359150506401000000008111156101de57600080fd5b8201836020820111156101f057600080fd5b8035906020019184600183028401116401000000008311171561021257600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092955061037d945050505050565b6100a06004803603604081101561026957600080fd5b6001600160a01b03823516919081019060408101602082013564010000000081111561029457600080fd5b8201836020820111156102a657600080fd5b803590602001918460018302840111640100000000831117156102c857600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295506105eb945050505050565b6100a06106ac565b6000546001600160a01b031681565b6004546001600160a01b031681565b600154600254600091610353916001600160a01b03918216919081169085166106bb565b92915050565b6002546001600160a01b031681565b60035481565b6005546001600160a01b031681565b60008082336040516020018080602001836001600160a01b03166001600160a01b03168152602001828103825284818151815260200191508051906020019080838360005b838110156103da5781810151838201526020016103c2565b50505050905090810190601f1680156104075780820380516001836020036101000a031916815260200191505b5093505050506040516020818303038152906040528051906020012090506060600560009054906101000a90046001600160a01b0316600260009054906101000a90046001600160a01b0316600354600160009054906101000a90046001600160a01b03168a8a8a60405160240180886001600160a01b03166001600160a01b03168152602001876001600160a01b03166001600160a01b03168152602001868152602001856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b0316815260200183815260200180602001828103825283818151815260200191508051906020019060200280838360005b8381101561051e578181015183820152602001610506565b50506040805193909501838103601f190184529094525060208101805163f658af4560e01b6001600160e01b0390911617905260008054919d509b506105839a5061057c99506001600160a01b031697506107279650505050505050565b8385610779565b9050876001600160a01b03166105988261032f565b600054604080516001600160a01b039283168152905192821692918516917f7bb36c64b37ae129eda8a24fd78defec04cc7a06bb27863c5a4571dd5d70acee9181900360200190a4979650505050505050565b604080516001600160a01b0384168183015260208082019283528351606083015283516000938493869388938392608001918601908083838a5b8381101561063d578181015183820152602001610625565b50505050905090810190601f16801561066a5780820380516001836020036101000a031916815260200191505b5060408051601f1981840301815291905280516020909101206000549095506106a494506001600160a01b031692503091508490506106bb565b949350505050565b6001546001600160a01b031681565b6000806106c785610727565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b604882015290565b825160009082816020870184f591506001600160a01b0382166107da576040805162461bcd60e51b8152602060048201526014602482015273195c9c9bdc97d85b1c9958591e50dc99585d195960621b604482015290519081900360640190fd5b8351156108d3576000826001600160a01b0316856040518082805190602001908083835b6020831061081d5780518252601f1990920191602091820191016107fe565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d806000811461087f576040519150601f19603f3d011682016040523d82523d6000602084013e610884565b606091505b50509050806108d1576040805162461bcd60e51b815260206004820152601460248201527332b93937b92fb4b734ba34b0b634bd30ba34b7b760611b604482015290519081900360640190fd5b505b50939250505056fea26469706673582212203d6bd28b39b5110f46fc2295d3ef797af10f4c6fcf6e1cec2eec5b605736142f64736f6c63430006060033";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_DATA_UNION_MAINNET_TEMPLATE = "data_union_mainnet_template";

    public static final String FUNC_DATA_UNION_SIDECHAIN_FACTORY = "data_union_sidechain_factory";

    public static final String FUNC_DATA_UNION_SIDECHAIN_TEMPLATE = "data_union_sidechain_template";

    public static final String FUNC_SIDECHAIN_MAXGAS = "sidechain_maxgas";

    public static final String FUNC_TOKEN_MEDIATOR = "token_mediator";

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

    public RemoteFunctionCall<Address> amb() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AMB, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> data_union_mainnet_template() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATA_UNION_MAINNET_TEMPLATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> data_union_sidechain_factory() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATA_UNION_SIDECHAIN_FACTORY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> data_union_sidechain_template() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATA_UNION_SIDECHAIN_TEMPLATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> sidechain_maxgas() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAIN_MAXGAS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> token_mediator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKEN_MEDIATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> sidechainAddress(Address mainet_address) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINADDRESS, 
                Arrays.<Type>asList(mainet_address), 
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

    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, Address _token_mediator, Address _data_union_mainnet_template, Address _data_union_sidechain_template, Address _data_union_sidechain_factory, Uint256 _sidechain_maxgas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_mainnet_template, _data_union_sidechain_template, _data_union_sidechain_factory, _sidechain_maxgas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, Address _token_mediator, Address _data_union_mainnet_template, Address _data_union_sidechain_template, Address _data_union_sidechain_factory, Uint256 _sidechain_maxgas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_mainnet_template, _data_union_sidechain_template, _data_union_sidechain_factory, _sidechain_maxgas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, Address _token_mediator, Address _data_union_mainnet_template, Address _data_union_sidechain_template, Address _data_union_sidechain_factory, Uint256 _sidechain_maxgas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_mainnet_template, _data_union_sidechain_template, _data_union_sidechain_factory, _sidechain_maxgas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactoryMainnet> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, Address _token_mediator, Address _data_union_mainnet_template, Address _data_union_sidechain_template, Address _data_union_sidechain_factory, Uint256 _sidechain_maxgas) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_mainnet_template, _data_union_sidechain_template, _data_union_sidechain_factory, _sidechain_maxgas));
        return deployRemoteCall(DataUnionFactoryMainnet.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class MainnetDUCreatedEventResponse extends BaseEventResponse {
        public Address mainnet;

        public Address sidechain;

        public Address owner;

        public Address template;
    }
}
