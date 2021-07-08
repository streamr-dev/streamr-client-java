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
 * <p>Generated with web3j version 4.8.4.
 */
@SuppressWarnings("rawtypes")
public class DataUnionFactorySidechain extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051610dff380380610dff8339818101604052604081101561003357600080fd5b508051602090910151600080546001600160a01b03199081163317909155600680546001600160a01b0394851690831617905560028054939092169216919091179055610d7a806100856000396000f3fe6080604052600436106100f75760003560e01c8063afc6224b1161008a578063f0ef0b0611610059578063f0ef0b0614610322578063f2fde38b1461034c578063f7c1329e1461037f578063fc0c546a14610394576100fe565b8063afc6224b146102bc578063cfeef807146102e3578063e22ab5ae146102f8578063e30c39781461030d576100fe565b806337dd8b05116100c657806337dd8b051461023c5780634e51a863146102685780634e71e0c8146102925780638da5cb5b146102a7576100fe565b80631062b39a1461010357806317c2a98c14610134578063187ac4cb14610167578063325ff66f1461017c576100fe565b366100fe57005b600080fd5b34801561010f57600080fd5b506101186103a9565b604080516001600160a01b039092168252519081900360200190f35b34801561014057600080fd5b506101186004803603602081101561015757600080fd5b50356001600160a01b031661048d565b34801561017357600080fd5b506101186104b2565b34801561018857600080fd5b506101186004803603604081101561019f57600080fd5b6001600160a01b0382351691908101906040810160208201356401000000008111156101ca57600080fd5b8201836020820111156101dc57600080fd5b803590602001918460208302840111640100000000831117156101fe57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506104c1945050505050565b34801561024857600080fd5b506102666004803603602081101561025f57600080fd5b50356107bd565b005b34801561027457600080fd5b506102666004803603602081101561028b57600080fd5b5035610853565b34801561029e57600080fd5b506102666108e8565b3480156102b357600080fd5b5061011861099e565b3480156102c857600080fd5b506102d16109ad565b60408051918252519081900360200190f35b3480156102ef57600080fd5b506101186109b3565b34801561030457600080fd5b506102d16109c2565b34801561031957600080fd5b506101186109c8565b34801561032e57600080fd5b506102666004803603602081101561034557600080fd5b50356109d7565b34801561035857600080fd5b506102666004803603602081101561036f57600080fd5b50356001600160a01b0316610a6c565b34801561038b57600080fd5b506102d1610ad9565b3480156103a057600080fd5b50610118610adf565b6006546040805163533426d160e01b815290516000926001600160a01b03169163533426d1916004808301926020929190829003018186803b1580156103ee57600080fd5b505afa158015610402573d6000803e3d6000fd5b505050506040513d602081101561041857600080fd5b50516040805163cd59658360e01b815290516001600160a01b039092169163cd59658391600480820192602092909190829003018186803b15801561045c57600080fd5b505afa158015610470573d6000803e3d6000fd5b505050506040513d602081101561048657600080fd5b5051905090565b6002546000906104ac906001600160a01b039081169030908516610b24565b92915050565b6006546001600160a01b031681565b60006104cb6103a9565b6001600160a01b0316336001600160a01b03161461051b576040805162461bcd60e51b815260206004820152600860248201526737b7363cafa0a6a160c11b604482015290519081900360640190fd5b60006105256103a9565b6001600160a01b031663d67bdd256040518163ffffffff1660e01b815260040160206040518083038186803b15801561055d57600080fd5b505afa158015610571573d6000803e3d6000fd5b505050506040513d602081101561058757600080fd5b50516006546005546040516001600160a01b0388811660248301908152938116604483018190529085166084830181905260a4830184905260a060648401908152895160c4850152895196975090956060958b9593948b948a949093909160e401906020878101910280838360005b8381101561060e5781810151838201526020016105f6565b50506040805193909501838103601f1901845290945250602081018051634d6b976f60e01b6001600160e01b03909116179052600254909a5060009950610672985061066b97506001600160a01b03169550610b90945050505050565b8385610be2565b600254604080516001600160a01b0392831681529051929350818a1692828516928816917f90d0a5d098b9a181ff8ddc866f840cc210e5b91eaf27bc267d5822a0deafad25919081900360200190a4600354158015906106d457506003544710155b1561073a576003546040516001600160a01b0383169180156108fc02916000818181858888f193505050501561073a5760035460408051918252517f517165f169759cdb94227d1c50f4f47895eb099a7f04a780f519bf1739face6f9181900360200190a15b6004541580159061074d57506004544710155b156107b3576004546040516001600160a01b0389169180156108fc02916000818181858888f19350505050156107b35760045460408051918252517f69e30c0bf438d0d3e0afb7f68d57ef394a0d5e8712f82fa00aa599e42574bc2a9181900360200190a15b9695505050505050565b6000546001600160a01b03163314610808576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b60055481141561081757610850565b60058190556040805182815290517f7a78bdfbfb2e909f35c05c77e80038cfd0a22c704748eba8b1d20aab76cd5d9c9181900360200190a15b50565b6000546001600160a01b0316331461089e576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b6003548114156108ad57610850565b60038190556040805182815290517fa02ce31a8a8adcdc2e2811a0c7f5d1eb1aa920ca9fdfaeaebfe3a2163e69a6549181900360200190a150565b6001546001600160a01b0316331461093a576040805162461bcd60e51b815260206004820152601060248201526f37b7363ca832b73234b733a7bbb732b960811b604482015290519081900360640190fd5b600154600080546040516001600160a01b0393841693909116917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a360018054600080546001600160a01b03199081166001600160a01b03841617909155169055565b6000546001600160a01b031681565b60045481565b6002546001600160a01b031681565b60055481565b6001546001600160a01b031681565b6000546001600160a01b03163314610a22576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600454811415610a3157610850565b60048190556040805182815290517fe08bf32e9c0e823a76d0088908afba678014c513e2311bba64fc72f38ae809709181900360200190a150565b6000546001600160a01b03163314610ab7576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b60035481565b6006546040805163836c081d60e01b815290516000926001600160a01b03169163836c081d916004808301926020929190829003018186803b15801561045c57600080fd5b600080610b3085610b90565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b604882015290565b825160009082816020870184f591506001600160a01b038216610c43576040805162461bcd60e51b8152602060048201526014602482015273195c9c9bdc97d85b1c9958591e50dc99585d195960621b604482015290519081900360640190fd5b835115610d3c576000826001600160a01b0316856040518082805190602001908083835b60208310610c865780518252601f199092019160209182019101610c67565b6001836020036101000a0380198251168184511680821785525050505050509050019150506000604051808303816000865af19150503d8060008114610ce8576040519150601f19603f3d011682016040523d82523d6000602084013e610ced565b606091505b5050905080610d3a576040805162461bcd60e51b815260206004820152601460248201527332b93937b92fb4b734ba34b0b634bd30ba34b7b760611b604482015290519081900360640190fd5b505b50939250505056fea2646970667358221220e10751fb6e1f2fd0035d38326971ac2be99e8073732656a09e684385d393c27864736f6c63430006060033";

    public static final String FUNC_CLAIMOWNERSHIP = "claimOwnership";

    public static final String FUNC_DATAUNIONSIDECHAINTEMPLATE = "dataUnionSidechainTemplate";

    public static final String FUNC_DEFAULTNEWMEMBERETH = "defaultNewMemberEth";

    public static final String FUNC_MIGRATIONMANAGER = "migrationManager";

    public static final String FUNC_NEWDUINITIALETH = "newDUInitialEth";

    public static final String FUNC_NEWDUOWNERINITIALETH = "newDUOwnerInitialEth";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PENDINGOWNER = "pendingOwner";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_TOKEN = "token";

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

    public RemoteFunctionCall<TransactionReceipt> claimOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLAIMOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Address> dataUnionSidechainTemplate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATAUNIONSIDECHAINTEMPLATE, 
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

    public RemoteFunctionCall<Address> migrationManager() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_MIGRATIONMANAGER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
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

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(Address newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(newOwner), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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

    public RemoteFunctionCall<Address> sidechainAddress(Address mainnetAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINADDRESS, 
                Arrays.<Type>asList(mainnetAddress), 
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

    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, Address _migrationManager, Address _dataUnionSidechainTemplate) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionSidechainTemplate));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, Address _migrationManager, Address _dataUnionSidechainTemplate) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionSidechainTemplate));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, Address _migrationManager, Address _dataUnionSidechainTemplate) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionSidechainTemplate));
        return deployRemoteCall(DataUnionFactorySidechain.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DataUnionFactorySidechain> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, Address _migrationManager, Address _dataUnionSidechainTemplate) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_migrationManager, _dataUnionSidechainTemplate));
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
