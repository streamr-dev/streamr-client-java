package com.streamr.client.dataunion.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
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
public class DataUnionMainnet extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50600080546001600160a01b0319169055611652806100306000396000f3fe608060405234801561001057600080fd5b50600436106101a95760003560e01c8063692199d4116100f9578063c5a8c91f11610097578063e30c397811610071578063e30c3978146103a7578063f2fde38b146103af578063fb6470c9146103d5578063fc0c546a146104b8576101a9565b8063c5a8c91f1461038f578063d35cec4014610397578063e22ec1121461039f576101a9565b806399dd1c81116100d357806399dd1c81146102d45780639eeba07c14610377578063a65c87d91461037f578063a913f41d14610387576101a9565b8063692199d4146102a75780638beb60b6146102af5780638da5cb5b146102cc576101a9565b80632efc1007116101665780634a439cc0116101405780634a439cc0146102515780634e71e0c81461028f57806354fd4d501461029757806361feacff1461029f576101a9565b80632efc10071461022557806337b43a941461022d578063392e53cd14610235576101a9565b80630419b45a146101ae5780630f3afcbe146101c85780631062b39a146101e9578063132b41941461020d57806313fd3c56146102155780632df3eba41461021d575b600080fd5b6101b66104c0565b60408051918252519081900360200190f35b6101e7600480360360208110156101de57600080fd5b50351515610606565b005b6101f1610664565b604080516001600160a01b039092168252519081900360200190f35b6101b6610673565b6101b6610708565b6101b6610721565b6101b6610727565b6101f1610d35565b61023d610d55565b604080519115158252519081900360200190f35b61023d600480360360a081101561026757600080fd5b508035906001600160a01b036020820135169060408101359060608101359060800135610d66565b6101e7610d7d565b6101b6610e33565b6101b6610e38565b6101b6610e3e565b6101e7600480360360208110156102c557600080fd5b5035610e44565b6101f1610f18565b6101e7600480360360208110156102ea57600080fd5b81019060208101813564010000000081111561030557600080fd5b82018360208201111561031757600080fd5b8035906020019184602083028401116401000000008311171561033957600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550610f27945050505050565b61023d6110cb565b6101f16110d4565b6101b66110e3565b6101f16110e9565b6101b66110f8565b6101f16110fe565b6101f161110d565b6101e7600480360360208110156103c557600080fd5b50356001600160a01b031661111c565b6101e760048036036101008110156103ec57600080fd5b6001600160a01b03823581169260208101358216926040820135831692606083013592608081013582169260a08201359092169160c08201359190810190610100810160e082013564010000000081111561044657600080fd5b82018360208201111561045857600080fd5b8035906020019184602083028401116401000000008311171561047a57600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550611189945050505050565b6101f16112f2565b6000806104cb610708565b9050806104dc576000915050610603565b600a546104ef908263ffffffff61130116565b600a55600654600080546040805163a9059cbb60e01b81526001600160a01b039283166004820152602481018690529051919093169263a9059cbb9260448083019360209390929083900390910190829087803b15801561054f57600080fd5b505af1158015610563573d6000803e3d6000fd5b505050506040513d602081101561057957600080fd5b50516105be576040805162461bcd60e51b815260206004820152600f60248201526e1d1c985b9cd9995c97d9985a5b1959608a1b604482015290519081900360640190fd5b6000546040805183815290516001600160a01b03909216917fcdcaff67ac16639664e5f9343c9223a1dc9c972ec367b69ae9fc1325c7be54749181900360200190a290505b90565b6000546001600160a01b03163314610651576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600b805460ff1916911515919091179055565b6002546001600160a01b031681565b6000610703610680610708565b600654604080516370a0823160e01b815230600482015290516001600160a01b03909216916370a0823191602480820192602092909190829003018186803b1580156106cb57600080fd5b505afa1580156106df573d6000803e3d6000fd5b505050506040513d60208110156106f557600080fd5b50519063ffffffff61136416565b905090565b6000610703600a5460095461136490919063ffffffff16565b600c5481565b600080610732610673565b905080610743576000915050610603565b6040805182815290517f41b06c6e0a1531dcb4b86d53ec6268666aa12d55775f8e5a63596fc935cdcc229181900360200190a160006107a5670de0b6b3a7640000610799600854856113a690919063ffffffff16565b9063ffffffff6113ff16565b905060006107b9838363ffffffff61136416565b6009549091506107cf908363ffffffff61130116565b6009556040805183815290517f538d1b2114be2374c7010694167f3db7f2d56f864a4e1555582b9716b7d11c3d9181900360200190a1600b5460ff161561081a576108186104c0565b505b6006546003546040805163095ea7b360e01b81526001600160a01b0392831660048201526000602482018190529151929093169263095ea7b39260448083019360209383900390910190829087803b15801561087557600080fd5b505af1158015610889573d6000803e3d6000fd5b505050506040513d602081101561089f57600080fd5b50516108e3576040805162461bcd60e51b815260206004820152600e60248201526d185c1c1c9bdd9957d9985a5b195960921b604482015290519081900360640190fd5b6006546003546040805163095ea7b360e01b81526001600160a01b039283166004820152602481018590529051919092169163095ea7b39160448083019260209291908290030181600087803b15801561093c57600080fd5b505af1158015610950573d6000803e3d6000fd5b505050506040513d602081101561096657600080fd5b50516109aa576040805162461bcd60e51b815260206004820152600e60248201526d185c1c1c9bdd9957d9985a5b195960921b604482015290519081900360640190fd5b6003546040805163437764df60e01b815290516000926001600160a01b03169163437764df916004808301926020929190829003018186803b1580156109ef57600080fd5b505afa158015610a03573d6000803e3d6000fd5b505050506040513d6020811015610a1957600080fd5b505190506358a8b61360e11b6001600160e01b031982161415610add576003546006546001600160a01b039182169163ad58bdd19116610a57610d35565b856040518463ffffffff1660e01b815260040180846001600160a01b03166001600160a01b03168152602001836001600160a01b03166001600160a01b031681526020018281526020019350505050600060405180830381600087803b158015610ac057600080fd5b505af1158015610ad4573d6000803e3d6000fd5b50505050610ba2565b633b2cadab60e11b6001600160e01b031982161415610b5f576003546001600160a01b03166301e4f53a610b0f610d35565b846040518363ffffffff1660e01b815260040180836001600160a01b03166001600160a01b0316815260200182815260200192505050600060405180830381600087803b158015610ac057600080fd5b6040805162461bcd60e51b8152602060048201526013602482015272756e6b6e6f776e5f6272696467655f6d6f646560681b604482015290519081900360640190fd5b610baa610673565b15610bee576040805162461bcd60e51b815260206004820152600f60248201526e1b9bdd17dd1c985b9cd9995c9c9959608a1b604482015290519081900360640190fd5b600c54610c01908363ffffffff61130116565b600c556040805160048152602481019091526020810180516001600160e01b031663331beb5f60e01b1790526002546001600160a01b031663dc8601b3610c46610d35565b6005546040516001600160e01b031960e085901b1681526001600160a01b038316600482019081526044820183905260606024830190815287516064840152875188949360840190602086019080838360005b83811015610cb1578181015183820152602001610c99565b50505050905090810190601f168015610cde5780820380516001836020036101000a031916815260200191505b50945050505050602060405180830381600087803b158015610cff57600080fd5b505af1158015610d13573d6000803e3d6000fd5b505050506040513d6020811015610d2957600080fd5b50949550505050505090565b600754600454600091610703916001600160a01b03918216911630611441565b6006546001600160a01b0316151590565b6000610d70610727565b5060019695505050505050565b6001546001600160a01b03163314610dcf576040805162461bcd60e51b815260206004820152601060248201526f37b7363ca832b73234b733a7bbb732b960811b604482015290519081900360640190fd5b600154600080546040516001600160a01b0393841693909116917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a360018054600080546001600160a01b03199081166001600160a01b03841617909155169055565b600290565b60095481565b60055481565b6000546001600160a01b03163314610e8f576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b670de0b6b3a7640000811115610edd576040805162461bcd60e51b815260206004820152600e60248201526d6572726f725f61646d696e46656560901b604482015290519081900360640190fd5b60088190556040805182815290517f11a80b766155f9b8f16a7da44d66269fd694cb1c247f4814244586f68dd534879181900360200190a150565b6000546001600160a01b031681565b60606000809054906101000a90046001600160a01b03168260405160240180836001600160a01b03166001600160a01b0316815260200180602001828103825283818151815260200191508051906020019060200280838360005b83811015610f9a578181015183820152602001610f82565b50506040805193909501838103601f190184528552505060208101805163325ff66f60e01b6001600160e01b0390911617815260025460048054600554965163dc8601b360e01b81526001600160a01b0391821692810183815260448201899052606060248301908152875160648401528751979e50929094169b5063dc8601b39a509198508b975091945090926084909101919080838360005b8381101561104d578181015183820152602001611035565b50505050905090810190601f16801561107a5780820380516001836020036101000a031916815260200191505b50945050505050602060405180830381600087803b15801561109b57600080fd5b505af11580156110af573d6000803e3d6000fd5b505050506040513d60208110156110c557600080fd5b50505050565b600b5460ff1681565b6007546001600160a01b031681565b600a5481565b6003546001600160a01b031681565b60085481565b6004546001600160a01b031681565b6001546001600160a01b031681565b6000546001600160a01b03163314611167576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b611191610d55565b156111cf576040805162461bcd60e51b8152602060048201526009602482015268696e69745f6f6e636560b81b604482015290519081900360640190fd5b600b805460ff19166001179055600080546001600160a01b03199081163317909155600380546001600160a01b038a81169190931617908190556040805163cd59658360e01b81529051919092169163cd596583916004808301926020929190829003018186803b15801561124357600080fd5b505afa158015611257573d6000803e3d6000fd5b505050506040513d602081101561126d57600080fd5b5051600280546001600160a01b03199081166001600160a01b03938416179091556006805482168b84161790556004805482168984161790556005879055600780549091169186169190911790556112c482610e44565b600080546001600160a01b0319166001600160a01b0385161790556112e881610f27565b5050505050505050565b6006546001600160a01b031681565b60008282018381101561135b576040805162461bcd60e51b815260206004820152601b60248201527f536166654d6174683a206164646974696f6e206f766572666c6f770000000000604482015290519081900360640190fd5b90505b92915050565b600061135b83836040518060400160405280601e81526020017f536166654d6174683a207375627472616374696f6e206f766572666c6f7700008152506114ad565b6000826113b55750600061135e565b828202828482816113c257fe5b041461135b5760405162461bcd60e51b81526004018080602001828103825260218152602001806115fc6021913960400191505060405180910390fd5b600061135b83836040518060400160405280601a81526020017f536166654d6174683a206469766973696f6e206279207a65726f000000000000815250611544565b60008061144d856115a9565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b6000818484111561153c5760405162461bcd60e51b81526004018080602001828103825283818151815260200191508051906020019080838360005b838110156115015781810151838201526020016114e9565b50505050905090810190601f16801561152e5780820380516001836020036101000a031916815260200191505b509250505060405180910390fd5b505050900390565b600081836115935760405162461bcd60e51b81526020600482018181528351602484015283519092839260449091019190850190808383600083156115015781810151838201526020016114e9565b50600083858161159f57fe5b0495945050505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b60488201529056fe536166654d6174683a206d756c7469706c69636174696f6e206f766572666c6f77a26469706673582212204e81cd126368697e91fa97ac7fcbbd4ccc8e290fb61449463779959d6807308364736f6c63430006060033";

    public static final String FUNC_ADMINFEEFRACTION = "adminFeeFraction";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_AUTOSENDADMINFEE = "autoSendAdminFee";

    public static final String FUNC_CLAIMOWNERSHIP = "claimOwnership";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PENDINGOWNER = "pendingOwner";

    public static final String FUNC_SIDECHAIN_DU_FACTORY = "sidechain_DU_factory";

    public static final String FUNC_SIDECHAIN_MAXGAS = "sidechain_maxgas";

    public static final String FUNC_SIDECHAIN_TEMPLATE_DU = "sidechain_template_DU";

    public static final String FUNC_TOKEN = "token";

    public static final String FUNC_TOKEN_MEDIATOR = "token_mediator";

    public static final String FUNC_TOTALADMINFEES = "totalAdminFees";

    public static final String FUNC_TOTALADMINFEESWITHDRAWN = "totalAdminFeesWithdrawn";

    public static final String FUNC_TOTALEARNINGS = "totalEarnings";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VERSION = "version";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISINITIALIZED = "isInitialized";

    public static final String FUNC_SETADMINFEE = "setAdminFee";

    public static final String FUNC_SETAUTOSENDADMINFEE = "setAutoSendAdminFee";

    public static final String FUNC_DEPLOYNEWDUSIDECHAIN = "deployNewDUSidechain";

    public static final String FUNC_SIDECHAINADDRESS = "sidechainAddress";

    public static final String FUNC_ONPURCHASE = "onPurchase";

    public static final String FUNC_ADMINFEESWITHDRAWABLE = "adminFeesWithdrawable";

    public static final String FUNC_UNACCOUNTEDTOKENS = "unaccountedTokens";

    public static final String FUNC_SENDTOKENSTOBRIDGE = "sendTokensToBridge";

    public static final String FUNC_WITHDRAWADMINFEES = "withdrawAdminFees";

    public static final Event ADMINFEECHANGED_EVENT = new Event("AdminFeeChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event ADMINFEECHARGED_EVENT = new Event("AdminFeeCharged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event ADMINFEESWITHDRAWN_EVENT = new Event("AdminFeesWithdrawn", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event REVENUERECEIVED_EVENT = new Event("RevenueReceived", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected DataUnionMainnet(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DataUnionMainnet(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DataUnionMainnet(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DataUnionMainnet(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<AdminFeeChangedEventResponse> getAdminFeeChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADMINFEECHANGED_EVENT, transactionReceipt);
        ArrayList<AdminFeeChangedEventResponse> responses = new ArrayList<AdminFeeChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AdminFeeChangedEventResponse typedResponse = new AdminFeeChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.adminFee = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AdminFeeChangedEventResponse> adminFeeChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AdminFeeChangedEventResponse>() {
            @Override
            public AdminFeeChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADMINFEECHANGED_EVENT, log);
                AdminFeeChangedEventResponse typedResponse = new AdminFeeChangedEventResponse();
                typedResponse.log = log;
                typedResponse.adminFee = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<AdminFeeChangedEventResponse> adminFeeChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADMINFEECHANGED_EVENT));
        return adminFeeChangedEventFlowable(filter);
    }

    public List<AdminFeeChargedEventResponse> getAdminFeeChargedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADMINFEECHARGED_EVENT, transactionReceipt);
        ArrayList<AdminFeeChargedEventResponse> responses = new ArrayList<AdminFeeChargedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AdminFeeChargedEventResponse typedResponse = new AdminFeeChargedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AdminFeeChargedEventResponse> adminFeeChargedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AdminFeeChargedEventResponse>() {
            @Override
            public AdminFeeChargedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADMINFEECHARGED_EVENT, log);
                AdminFeeChargedEventResponse typedResponse = new AdminFeeChargedEventResponse();
                typedResponse.log = log;
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<AdminFeeChargedEventResponse> adminFeeChargedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADMINFEECHARGED_EVENT));
        return adminFeeChargedEventFlowable(filter);
    }

    public List<AdminFeesWithdrawnEventResponse> getAdminFeesWithdrawnEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADMINFEESWITHDRAWN_EVENT, transactionReceipt);
        ArrayList<AdminFeesWithdrawnEventResponse> responses = new ArrayList<AdminFeesWithdrawnEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AdminFeesWithdrawnEventResponse typedResponse = new AdminFeesWithdrawnEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.admin = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AdminFeesWithdrawnEventResponse> adminFeesWithdrawnEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AdminFeesWithdrawnEventResponse>() {
            @Override
            public AdminFeesWithdrawnEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADMINFEESWITHDRAWN_EVENT, log);
                AdminFeesWithdrawnEventResponse typedResponse = new AdminFeesWithdrawnEventResponse();
                typedResponse.log = log;
                typedResponse.admin = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<AdminFeesWithdrawnEventResponse> adminFeesWithdrawnEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADMINFEESWITHDRAWN_EVENT));
        return adminFeesWithdrawnEventFlowable(filter);
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

    public List<RevenueReceivedEventResponse> getRevenueReceivedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REVENUERECEIVED_EVENT, transactionReceipt);
        ArrayList<RevenueReceivedEventResponse> responses = new ArrayList<RevenueReceivedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RevenueReceivedEventResponse typedResponse = new RevenueReceivedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RevenueReceivedEventResponse> revenueReceivedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RevenueReceivedEventResponse>() {
            @Override
            public RevenueReceivedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REVENUERECEIVED_EVENT, log);
                RevenueReceivedEventResponse typedResponse = new RevenueReceivedEventResponse();
                typedResponse.log = log;
                typedResponse.amount = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<RevenueReceivedEventResponse> revenueReceivedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REVENUERECEIVED_EVENT));
        return revenueReceivedEventFlowable(filter);
    }

    public RemoteFunctionCall<Uint256> adminFeeFraction() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ADMINFEEFRACTION, 
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

    public RemoteFunctionCall<Bool> autoSendAdminFee() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AUTOSENDADMINFEE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> claimOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLAIMOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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

    public RemoteFunctionCall<Address> sidechain_DU_factory() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAIN_DU_FACTORY, 
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

    public RemoteFunctionCall<Address> sidechain_template_DU() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAIN_TEMPLATE_DU, 
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

    public RemoteFunctionCall<Address> token_mediator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKEN_MEDIATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> totalAdminFees() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOTALADMINFEES, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> totalAdminFeesWithdrawn() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOTALADMINFEESWITHDRAWN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> totalEarnings() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOTALEARNINGS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(Address newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(newOwner), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Uint256> version() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(Address _token, Address _token_mediator, Address _sidechain_DU_factory, Uint256 _sidechain_maxgas, Address _sidechain_template_DU, Address _owner, Uint256 _adminFeeFraction, DynamicArray<Address> agents) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(_token, _token_mediator, _sidechain_DU_factory, _sidechain_maxgas, _sidechain_template_DU, _owner, _adminFeeFraction, agents), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Bool> isInitialized() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISINITIALIZED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAdminFee(Uint256 newAdminFee) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETADMINFEE, 
                Arrays.<Type>asList(newAdminFee), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAutoSendAdminFee(Bool autoSend) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETAUTOSENDADMINFEE, 
                Arrays.<Type>asList(autoSend), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deployNewDUSidechain(DynamicArray<Address> agents) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DEPLOYNEWDUSIDECHAIN, 
                Arrays.<Type>asList(agents), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Address> sidechainAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> onPurchase(Bytes32 param0, Address param1, Uint256 param2, Uint256 param3, Uint256 param4) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONPURCHASE, 
                Arrays.<Type>asList(param0, param1, param2, param3, param4), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Uint256> adminFeesWithdrawable() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ADMINFEESWITHDRAWABLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> unaccountedTokens() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_UNACCOUNTEDTOKENS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> sendTokensToBridge() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDTOKENSTOBRIDGE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdrawAdminFees() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_WITHDRAWADMINFEES, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static DataUnionMainnet load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionMainnet(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DataUnionMainnet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DataUnionMainnet(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DataUnionMainnet load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DataUnionMainnet(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DataUnionMainnet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DataUnionMainnet(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DataUnionMainnet> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DataUnionMainnet.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<DataUnionMainnet> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DataUnionMainnet.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DataUnionMainnet> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DataUnionMainnet.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DataUnionMainnet> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DataUnionMainnet.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class AdminFeeChangedEventResponse extends BaseEventResponse {
        public Uint256 adminFee;
    }

    public static class AdminFeeChargedEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }

    public static class AdminFeesWithdrawnEventResponse extends BaseEventResponse {
        public Address admin;

        public Uint256 amount;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public Address previousOwner;

        public Address newOwner;
    }

    public static class RevenueReceivedEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }
}
