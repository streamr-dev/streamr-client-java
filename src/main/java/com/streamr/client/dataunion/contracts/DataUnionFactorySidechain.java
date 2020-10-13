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
public class DataUnionFactorySidechain extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051610dc2380380610dc28339818101604052604081101561003357600080fd5b50805160209182015160008054336001600160a01b0319918216179091556004805482166001600160a01b038086169190911780835560028054909416828616179093556040805163cd59658360e01b8152905195969495939091169363cd59658393828401939192909190829003018186803b1580156100b357600080fd5b505afa1580156100c7573d6000803e3d6000fd5b505050506040513d60208110156100dd57600080fd5b5051600380546001600160a01b0319166001600160a01b0390921691909117905550610cb29050806101106000396000f3fe6080604052600436106100ec5760003560e01c8063afc6224b1161008a578063e4a154a411610059578063e4a154a414610302578063f0ef0b0614610317578063f2fde38b14610341578063f7c1329e14610374576100f3565b8063afc6224b1461029c578063c5a8c91f146102c3578063e22ab5ae146102d8578063e30c3978146102ed576100f3565b806337dd8b05116100c657806337dd8b051461021c5780634e51a863146102485780634e71e0c8146102725780638da5cb5b14610287576100f3565b80631062b39a146100f857806317c2a98c14610129578063325ff66f1461015c576100f3565b366100f357005b600080fd5b34801561010457600080fd5b5061010d610389565b604080516001600160a01b039092168252519081900360200190f35b34801561013557600080fd5b5061010d6004803603602081101561014c57600080fd5b50356001600160a01b0316610398565b34801561016857600080fd5b5061010d6004803603604081101561017f57600080fd5b6001600160a01b0382351691908101906040810160208201356401000000008111156101aa57600080fd5b8201836020820111156101bc57600080fd5b803590602001918460208302840111640100000000831117156101de57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506103bd945050505050565b34801561022857600080fd5b506102466004803603602081101561023f57600080fd5b503561072b565b005b34801561025457600080fd5b506102466004803603602081101561026b57600080fd5b50356107c1565b34801561027e57600080fd5b50610246610856565b34801561029357600080fd5b5061010d61090c565b3480156102a857600080fd5b506102b161091b565b60408051918252519081900360200190f35b3480156102cf57600080fd5b5061010d610921565b3480156102e457600080fd5b506102b1610930565b3480156102f957600080fd5b5061010d610936565b34801561030e57600080fd5b5061010d610945565b34801561032357600080fd5b506102466004803603602081101561033a57600080fd5b5035610954565b34801561034d57600080fd5b506102466004803603602081101561036457600080fd5b50356001600160a01b03166109e9565b34801561038057600080fd5b506102b1610a56565b6003546001600160a01b031681565b6002546000906103b7906001600160a01b039081169030908516610a5c565b92915050565b6003546000906001600160a01b0316331461040a576040805162461bcd60e51b815260206004820152600860248201526737b7363cafa0a6a160c11b604482015290519081900360640190fd5b6003546040805163d67bdd2560e01b815290516000926001600160a01b03169163d67bdd25916004808301926020929190829003018186803b15801561044f57600080fd5b505afa158015610463573d6000803e3d6000fd5b505050506040513d602081101561047957600080fd5b505160048054604080516318d8f9c960e01b815290519394506001600160a01b03808616946060948a94909216926318d8f9c99281810192602092909190829003018186803b1580156104cb57600080fd5b505afa1580156104df573d6000803e3d6000fd5b505050506040513d60208110156104f557600080fd5b50516004546007546040516001600160a01b038581166024830190815281861660448401529381166084830181905290891660a483015260c4820183905260c0606483019081528b5160e48401528b518c9592948b9490939291610104909101906020888101910280838360005b8381101561057b578181015183820152602001610563565b50506040805193909501838103601f190184529094525060208101805163015c7f5160e01b6001600160e01b03909116179052600254909b5060009a506105e099506105d998506001600160a01b03169650610ac895505050505050565b8385610b1a565b600254604080516001600160a01b0392831681529051929350818a1692828516928816917f90d0a5d098b9a181ff8ddc866f840cc210e5b91eaf27bc267d5822a0deafad25919081900360200190a46005541580159061064257506005544710155b156106a8576005546040516001600160a01b0383169180156108fc02916000818181858888f19350505050156106a85760055460408051918252517f517165f169759cdb94227d1c50f4f47895eb099a7f04a780f519bf1739face6f9181900360200190a15b600654158015906106bb57506006544710155b15610721576006546040516001600160a01b0389169180156108fc02916000818181858888f19350505050156107215760065460408051918252517f69e30c0bf438d0d3e0afb7f68d57ef394a0d5e8712f82fa00aa599e42574bc2a9181900360200190a15b9695505050505050565b6000546001600160a01b03163314610776576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600754811415610785576107be565b60078190556040805182815290517f7a78bdfbfb2e909f35c05c77e80038cfd0a22c704748eba8b1d20aab76cd5d9c9181900360200190a15b50565b6000546001600160a01b0316331461080c576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b60055481141561081b576107be565b60058190556040805182815290517fa02ce31a8a8adcdc2e2811a0c7f5d1eb1aa920ca9fdfaeaebfe3a2163e69a6549181900360200190a150565b6001546001600160a01b031633146108a8576040805162461bcd60e51b815260206004820152601060248201526f37b7363ca832b73234b733a7bbb732b960811b604482015290519081900360640190fd5b600154600080546040516001600160a01b0393841693909116917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a360018054600080546001600160a01b03199081166001600160a01b03841617909155169055565b6000546001600160a01b031681565b60065481565b6004546001600160a01b031681565b60075481565b6001546001600160a01b031681565b6002546001600160a01b031681565b6000546001600160a01b0316331461099f576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b6006548114156109ae576107be565b60068190556040805182815290517fe08bf32e9c0e823a76d0088908afba678014c513e2311bba64fc72f38ae809709181900360200190a150565b6000546001600160a01b03163314610a34576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b60055481565b600080610a6885610ac8565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b604882015290565b825160009082816020870184f591506001600160a01b038216610b7b576040805162461bcd60e51b8152602060048201526014602482015273195c9c9bdc97d85b1c9958591e50dc99585d195960621b604482015290519081900360640190fd5b835115610c74576000826001600160a01b0316856040518082805190602001908083835b60208310610bbe5780518252601f199092019160209182019101610b9f565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d8060008114610c20576040519150601f19603f3d011682016040523d82523d6000602084013e610c25565b606091505b5050905080610c72576040805162461bcd60e51b815260206004820152601460248201527332b93937b92fb4b734ba34b0b634bd30ba34b7b760611b604482015290519081900360640190fd5b505b50939250505056fea2646970667358221220505ab6d8409b3af0464e7a826bfb322c476197ae31cb01c9dc299dedea19d9d664736f6c63430006060033";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_CLAIMOWNERSHIP = "claimOwnership";

    public static final String FUNC_DATA_UNION_SIDECHAIN_TEMPLATE = "data_union_sidechain_template";

    public static final String FUNC_DEFAULTNEWMEMBERETH = "defaultNewMemberEth";

    public static final String FUNC_NEWDUINITIALETH = "newDUInitialEth";

    public static final String FUNC_NEWDUOWNERINITIALETH = "newDUOwnerInitialEth";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PENDINGOWNER = "pendingOwner";

    public static final String FUNC_TOKEN_MEDIATOR = "token_mediator";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_SETNEWDUINITIALETH = "setNewDUInitialEth";

    public static final String FUNC_SETNEWDUOWNERINITIALETH = "setNewDUOwnerInitialEth";

    public static final String FUNC_SETNEWMEMBERINITIALETH = "setNewMemberInitialEth";

    public static final String FUNC_SIDECHAINADDRESS = "sidechainAddress";

    public static final String FUNC_DEPLOYNEWDUSIDECHAIN = "deployNewDUSidechain";

    public static final Event DUINITIALETHSENT_EVENT = new Event("DUInitialEthSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERINITIALETHSENT_EVENT = new Event("OwnerInitialEthSent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event SIDECHAINDUCREATED_EVENT = new Event("SidechainDUCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>() {}));
    ;

    public static final Event UPDATEDEFAULTNEWMEMBERINITIALETH_EVENT = new Event("UpdateDefaultNewMemberInitialEth", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event UPDATENEWDUINITIALETH_EVENT = new Event("UpdateNewDUInitialEth", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event UPDATENEWDUOWNERINITIALETH_EVENT = new Event("UpdateNewDUOwnerInitialEth", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected DataUnionFactorySidechain(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DataUnionFactorySidechain(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DataUnionFactorySidechain(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DataUnionFactorySidechain(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<DUInitialEthSentEventResponse> getDUInitialEthSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DUINITIALETHSENT_EVENT, transactionReceipt);
        ArrayList<DUInitialEthSentEventResponse> responses = new ArrayList<DUInitialEthSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DUInitialEthSentEventResponse typedResponse = new DUInitialEthSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amountWei = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DUInitialEthSentEventResponse> dUInitialEthSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DUInitialEthSentEventResponse>() {
            @Override
            public DUInitialEthSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DUINITIALETHSENT_EVENT, log);
                DUInitialEthSentEventResponse typedResponse = new DUInitialEthSentEventResponse();
                typedResponse.log = log;
                typedResponse.amountWei = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<DUInitialEthSentEventResponse> dUInitialEthSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DUINITIALETHSENT_EVENT));
        return dUInitialEthSentEventFlowable(filter);
    }

    public List<OwnerInitialEthSentEventResponse> getOwnerInitialEthSentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERINITIALETHSENT_EVENT, transactionReceipt);
        ArrayList<OwnerInitialEthSentEventResponse> responses = new ArrayList<OwnerInitialEthSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerInitialEthSentEventResponse typedResponse = new OwnerInitialEthSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amountWei = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnerInitialEthSentEventResponse> ownerInitialEthSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OwnerInitialEthSentEventResponse>() {
            @Override
            public OwnerInitialEthSentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERINITIALETHSENT_EVENT, log);
                OwnerInitialEthSentEventResponse typedResponse = new OwnerInitialEthSentEventResponse();
                typedResponse.log = log;
                typedResponse.amountWei = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<OwnerInitialEthSentEventResponse> ownerInitialEthSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERINITIALETHSENT_EVENT));
        return ownerInitialEthSentEventFlowable(filter);
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.newOwner = (Address) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.newOwner = (Address) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public List<SidechainDUCreatedEventResponse> getSidechainDUCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SIDECHAINDUCREATED_EVENT, transactionReceipt);
        ArrayList<SidechainDUCreatedEventResponse> responses = new ArrayList<SidechainDUCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SidechainDUCreatedEventResponse typedResponse = new SidechainDUCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.mainnet = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.sidenet = (Address) eventValues.getIndexedValues().get(1);
            typedResponse.owner = (Address) eventValues.getIndexedValues().get(2);
            typedResponse.template = (Address) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SidechainDUCreatedEventResponse> sidechainDUCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SidechainDUCreatedEventResponse>() {
            @Override
            public SidechainDUCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SIDECHAINDUCREATED_EVENT, log);
                SidechainDUCreatedEventResponse typedResponse = new SidechainDUCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.mainnet = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.sidenet = (Address) eventValues.getIndexedValues().get(1);
                typedResponse.owner = (Address) eventValues.getIndexedValues().get(2);
                typedResponse.template = (Address) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<SidechainDUCreatedEventResponse> sidechainDUCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SIDECHAINDUCREATED_EVENT));
        return sidechainDUCreatedEventFlowable(filter);
    }

    public List<UpdateDefaultNewMemberInitialEthEventResponse> getUpdateDefaultNewMemberInitialEthEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPDATEDEFAULTNEWMEMBERINITIALETH_EVENT, transactionReceipt);
        ArrayList<UpdateDefaultNewMemberInitialEthEventResponse> responses = new ArrayList<UpdateDefaultNewMemberInitialEthEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpdateDefaultNewMemberInitialEthEventResponse typedResponse = new UpdateDefaultNewMemberInitialEthEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpdateDefaultNewMemberInitialEthEventResponse> updateDefaultNewMemberInitialEthEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, UpdateDefaultNewMemberInitialEthEventResponse>() {
            @Override
            public UpdateDefaultNewMemberInitialEthEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPDATEDEFAULTNEWMEMBERINITIALETH_EVENT, log);
                UpdateDefaultNewMemberInitialEthEventResponse typedResponse = new UpdateDefaultNewMemberInitialEthEventResponse();
                typedResponse.log = log;
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<UpdateDefaultNewMemberInitialEthEventResponse> updateDefaultNewMemberInitialEthEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPDATEDEFAULTNEWMEMBERINITIALETH_EVENT));
        return updateDefaultNewMemberInitialEthEventFlowable(filter);
    }

    public List<UpdateNewDUInitialEthEventResponse> getUpdateNewDUInitialEthEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPDATENEWDUINITIALETH_EVENT, transactionReceipt);
        ArrayList<UpdateNewDUInitialEthEventResponse> responses = new ArrayList<UpdateNewDUInitialEthEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpdateNewDUInitialEthEventResponse typedResponse = new UpdateNewDUInitialEthEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpdateNewDUInitialEthEventResponse> updateNewDUInitialEthEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, UpdateNewDUInitialEthEventResponse>() {
            @Override
            public UpdateNewDUInitialEthEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPDATENEWDUINITIALETH_EVENT, log);
                UpdateNewDUInitialEthEventResponse typedResponse = new UpdateNewDUInitialEthEventResponse();
                typedResponse.log = log;
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<UpdateNewDUInitialEthEventResponse> updateNewDUInitialEthEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPDATENEWDUINITIALETH_EVENT));
        return updateNewDUInitialEthEventFlowable(filter);
    }

    public List<UpdateNewDUOwnerInitialEthEventResponse> getUpdateNewDUOwnerInitialEthEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPDATENEWDUOWNERINITIALETH_EVENT, transactionReceipt);
        ArrayList<UpdateNewDUOwnerInitialEthEventResponse> responses = new ArrayList<UpdateNewDUOwnerInitialEthEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpdateNewDUOwnerInitialEthEventResponse typedResponse = new UpdateNewDUOwnerInitialEthEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpdateNewDUOwnerInitialEthEventResponse> updateNewDUOwnerInitialEthEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, UpdateNewDUOwnerInitialEthEventResponse>() {
            @Override
            public UpdateNewDUOwnerInitialEthEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPDATENEWDUOWNERINITIALETH_EVENT, log);
                UpdateNewDUOwnerInitialEthEventResponse typedResponse = new UpdateNewDUOwnerInitialEthEventResponse();
                typedResponse.log = log;
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<UpdateNewDUOwnerInitialEthEventResponse> updateNewDUOwnerInitialEthEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPDATENEWDUOWNERINITIALETH_EVENT));
        return updateNewDUOwnerInitialEthEventFlowable(filter);
    }

    public RemoteFunctionCall<Address> amb() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AMB, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLAIMOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Address> data_union_sidechain_template() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATA_UNION_SIDECHAIN_TEMPLATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> defaultNewMemberEth() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DEFAULTNEWMEMBERETH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> newDUInitialEth() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NEWDUINITIALETH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> newDUOwnerInitialEth() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NEWDUOWNERINITIALETH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> pendingOwner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PENDINGOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> token_mediator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKEN_MEDIATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(Address newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(newOwner), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setNewDUInitialEth(Uint256 val) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETNEWDUINITIALETH, 
                Arrays.<Type>asList(val), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setNewDUOwnerInitialEth(Uint256 val) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETNEWDUOWNERINITIALETH, 
                Arrays.<Type>asList(val), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setNewMemberInitialEth(Uint256 val) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETNEWMEMBERINITIALETH, 
                Arrays.<Type>asList(val), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Address> sidechainAddress(Address mainet_address) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINADDRESS, 
                Arrays.<Type>asList(mainet_address), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deployNewDUSidechain(Address owner, DynamicArray<Address> agents) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DEPLOYNEWDUSIDECHAIN, 
                Arrays.<Type>asList(owner, agents), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static DataUnionFactorySidechain load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionFactorySidechain(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DataUnionFactorySidechain load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionFactorySidechain(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DataUnionFactorySidechain load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DataUnionFactorySidechain(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DataUnionFactorySidechain load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DataUnionFactorySidechain(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, Address _token_mediator, Address _data_union_sidechain_template) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_sidechain_template));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, Address _token_mediator, Address _data_union_sidechain_template) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_sidechain_template));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, Address _token_mediator, Address _data_union_sidechain_template) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_sidechain_template));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, Address _token_mediator, Address _data_union_sidechain_template) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_token_mediator, _data_union_sidechain_template));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class DUInitialEthSentEventResponse extends BaseEventResponse {
        public Uint256 amountWei;
    }

    public static class OwnerInitialEthSentEventResponse extends BaseEventResponse {
        public Uint256 amountWei;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public Address previousOwner;

        public Address newOwner;
    }

    public static class SidechainDUCreatedEventResponse extends BaseEventResponse {
        public Address mainnet;

        public Address sidenet;

        public Address owner;

        public Address template;
    }

    public static class UpdateDefaultNewMemberInitialEthEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }

    public static class UpdateNewDUInitialEthEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }

    public static class UpdateNewDUOwnerInitialEthEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }
}
