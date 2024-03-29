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
import org.web3j.abi.datatypes.DynamicBytes;
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
 * <p>Generated with web3j version 4.8.4.
 */
@SuppressWarnings("rawtypes")
public class DataUnionMainnet extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50600080546001600160a01b0319169055611881806100306000396000f3fe608060405234801561001057600080fd5b50600436106101da5760003560e01c80638beb60b611610104578063cb12b92d116100a2578063e30c397811610071578063e30c39781461046d578063f2fde38b14610475578063f658af451461049b578063fc0c546a14610574576101da565b8063cb12b92d1461044d578063cc77244014610455578063d35cec401461045d578063d9c8c63b14610465576101da565b806399dd1c81116100de57806399dd1c81146103155780639eeba07c146103b8578063a4c0ed36146103c0578063a913f41d14610445576101da565b80638beb60b6146102e85780638da5cb5b146103055780638fd3ab801461030d576101da565b80632efc10071161017c5780634e71e0c81161014b5780634e71e0c8146102c857806354fd4d50146102d057806361feacff146102d857806375ddc11d146102e0576101da565b80632efc10071461025e57806337b43a9414610266578063392e53cd1461026e5780634a439cc01461028a576101da565b80631062b39a116101b85780631062b39a14610222578063132b41941461024657806313fd3c561461024e578063187ac4cb14610256576101da565b80630419b45a146101df5780630b23e95a146101f95780630f3afcbe14610201575b600080fd5b6101e761057c565b60408051918252519081900360200190f35b6101e76106c2565b6102206004803603602081101561021757600080fd5b503515156106c8565b005b61022a610726565b604080516001600160a01b039092168252519081900360200190f35b6101e761079c565b6101e7610831565b61022a61084a565b6101e7610859565b61022a610c77565b610276610c97565b604080519115158252519081900360200190f35b610276600480360360a08110156102a057600080fd5b508035906001600160a01b036020820135169060408101359060608101359060800135610ca8565b610220610cbf565b6101e7610d75565b6101e7610d7a565b61022a610d80565b610220600480360360208110156102fe57600080fd5b5035610d8f565b61022a610e63565b610220610e72565b6102206004803603602081101561032b57600080fd5b81019060208101813564010000000081111561034657600080fd5b82018360208201111561035857600080fd5b8035906020019184602083028401116401000000008311171561037a57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295506110ab945050505050565b610276611258565b610276600480360360608110156103d657600080fd5b6001600160a01b038235169160208101359181019060608101604082013564010000000081111561040657600080fd5b82018360208201111561041857600080fd5b8035906020019184600183028401116401000000008311171561043a57600080fd5b509092509050611261565b6101e7611294565b6101e761129a565b61022a6112a0565b6101e76112af565b61022a6112b5565b61022a6112c4565b6102206004803603602081101561048b57600080fd5b50356001600160a01b03166112d3565b610220600480360360e08110156104b157600080fd5b6001600160a01b0382358116926020810135821692604082013592606083013581169260808101359091169160a0820135919081019060e0810160c082013564010000000081111561050257600080fd5b82018360208201111561051457600080fd5b8035906020019184602083028401116401000000008311171561053657600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550611340945050505050565b61022a611521565b600080610587610831565b9050806105985760009150506106bf565b600a546105ab908263ffffffff61153016565b600a55600554600080546040805163a9059cbb60e01b81526001600160a01b039283166004820152602481018690529051919093169263a9059cbb9260448083019360209390929083900390910190829087803b15801561060b57600080fd5b505af115801561061f573d6000803e3d6000fd5b505050506040513d602081101561063557600080fd5b505161067a576040805162461bcd60e51b815260206004820152600f60248201526e1d1c985b9cd9995c97d9985a5b1959608a1b604482015290519081900360640190fd5b6000546040805183815290516001600160a01b03909216917fcdcaff67ac16639664e5f9343c9223a1dc9c972ec367b69ae9fc1325c7be54749181900360200190a290505b90565b60045481565b6000546001600160a01b03163314610713576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600b805460ff1916911515919091179055565b6002546040805163cd59658360e01b815290516000926001600160a01b03169163cd596583916004808301926020929190829003018186803b15801561076b57600080fd5b505afa15801561077f573d6000803e3d6000fd5b505050506040513d602081101561079557600080fd5b5051905090565b600061082c6107a9610831565b600554604080516370a0823160e01b815230600482015290516001600160a01b03909216916370a0823191602480820192602092909190829003018186803b1580156107f457600080fd5b505afa158015610808573d6000803e3d6000fd5b505050506040513d602081101561081e57600080fd5b50519063ffffffff61159316565b905090565b600061082c600a5460095461159390919063ffffffff16565b6006546001600160a01b031681565b60008061086461079c565b9050806108755760009150506106bf565b6040805182815290517f41b06c6e0a1531dcb4b86d53ec6268666aa12d55775f8e5a63596fc935cdcc229181900360200190a160006108d7670de0b6b3a76400006108cb600854856115d590919063ffffffff16565b9063ffffffff61162e16565b905060006108eb838363ffffffff61159316565b600954909150610901908363ffffffff61153016565b6009556040805183815290517f538d1b2114be2374c7010694167f3db7f2d56f864a4e1555582b9716b7d11c3d9181900360200190a1600b5460ff161561094c5761094a61057c565b505b6005546002546040805163095ea7b360e01b81526001600160a01b0392831660048201526000602482018190529151929093169263095ea7b39260448083019360209383900390910190829087803b1580156109a757600080fd5b505af11580156109bb573d6000803e3d6000fd5b505050506040513d60208110156109d157600080fd5b5051610a15576040805162461bcd60e51b815260206004820152600e60248201526d185c1c1c9bdd9957d9985a5b195960921b604482015290519081900360640190fd5b6005546002546040805163095ea7b360e01b81526001600160a01b039283166004820152602481018590529051919092169163095ea7b39160448083019260209291908290030181600087803b158015610a6e57600080fd5b505af1158015610a82573d6000803e3d6000fd5b505050506040513d6020811015610a9857600080fd5b5051610adc576040805162461bcd60e51b815260206004820152600e60248201526d185c1c1c9bdd9957d9985a5b195960921b604482015290519081900360640190fd5b6002546005546001600160a01b039182169163d74054819116610afd610c77565b84604051602001808062222a9960e91b81525060030190506040516020818303038152906040526040518563ffffffff1660e01b815260040180856001600160a01b03166001600160a01b03168152602001846001600160a01b03166001600160a01b0316815260200183815260200180602001828103825283818151815260200191508051906020019080838360005b83811015610ba6578181015183820152602001610b8e565b50505050905090810190601f168015610bd35780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b158015610bf557600080fd5b505af1158015610c09573d6000803e3d6000fd5b50505050610c1561079c565b15610c59576040805162461bcd60e51b815260206004820152600f60248201526e1b9bdd17dd1c985b9cd9995c9c9959608a1b604482015290519081900360640190fd5b600c54610c6c908263ffffffff61153016565b600c55509091505090565b60075460035460009161082c916001600160a01b03918216911630611670565b6005546001600160a01b0316151590565b6000610cb2610859565b5060019695505050505050565b6001546001600160a01b03163314610d11576040805162461bcd60e51b815260206004820152601060248201526f37b7363ca832b73234b733a7bbb732b960811b604482015290519081900360640190fd5b600154600080546040516001600160a01b0393841693909116917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a360018054600080546001600160a01b03199081166001600160a01b03841617909155169055565b600290565b60095481565b6007546001600160a01b031681565b6000546001600160a01b03163314610dda576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b670de0b6b3a7640000811115610e28576040805162461bcd60e51b815260206004820152600e60248201526d6572726f725f61646d696e46656560901b604482015290519081900360640190fd5b60088190556040805182815290517f11a80b766155f9b8f16a7da44d66269fd694cb1c247f4814244586f68dd534879181900360200190a150565b6000546001600160a01b031681565b6000546001600160a01b03163314610ebd576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b6006546040805163836c081d60e01b815290516000926001600160a01b03169163836c081d916004808301926020929190829003018186803b158015610f0257600080fd5b505afa158015610f16573d6000803e3d6000fd5b505050506040513d6020811015610f2c57600080fd5b505190506001600160a01b03811615801590610f5657506005546001600160a01b03828116911614155b15610fb2576005546040516001600160a01b03918216918316907f1a10a9f5e0b3bc7bffb82cdebe89204ade13ed81bf7dc816199fadc282de3d8a90600090a3600580546001600160a01b0319166001600160a01b0383161790555b6006546040805163533426d160e01b815290516000926001600160a01b03169163533426d1916004808301926020929190829003018186803b158015610ff757600080fd5b505afa15801561100b573d6000803e3d6000fd5b505050506040513d602081101561102157600080fd5b505190506001600160a01b0381161580159061104b57506002546001600160a01b03828116911614155b156110a7576002546040516001600160a01b03918216918316907f5d82b60ad3cf3639e02e96994b2b10060c4c0a7c0214695baa228363fb910c3490600090a3600280546001600160a01b0319166001600160a01b0383161790555b5050565b60606000809054906101000a90046001600160a01b03168260405160240180836001600160a01b03166001600160a01b0316815260200180602001828103825283818151815260200191508051906020019060200280838360005b8381101561111e578181015183820152602001611106565b50506040805193909501838103601f190184529094525060208101805163325ff66f60e01b6001600160e01b0390911617905296506111639550610726945050505050565b6003546004805460405163dc8601b360e01b81526001600160a01b0393841692810183815260448201839052606060248301908152875160648401528751969095169563dc8601b3958894939091608490910190602086019080838360005b838110156111da5781810151838201526020016111c2565b50505050905090810190601f1680156112075780820380516001836020036101000a031916815260200191505b50945050505050602060405180830381600087803b15801561122857600080fd5b505af115801561123c573d6000803e3d6000fd5b505050506040513d602081101561125257600080fd5b50505050565b600b5460ff1681565b6005546000906001600160a01b0316331461127e5750600061128c565b611286610859565b50600190505b949350505050565b600a5481565b600c5481565b6002546001600160a01b031681565b60085481565b6003546001600160a01b031681565b6001546001600160a01b031681565b6000546001600160a01b0316331461131e576040805162461bcd60e51b815260206004820152600960248201526837b7363ca7bbb732b960b91b604482015290519081900360640190fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b611348610c97565b15611386576040805162461bcd60e51b8152602060048201526009602482015268696e69745f6f6e636560b81b604482015290519081900360640190fd5b600b805460ff19166001179055600680546001600160a01b03808a166001600160a01b031992831617928390556000805490921633179091556040805163533426d160e01b81529051929091169163533426d191600480820192602092909190829003018186803b1580156113fa57600080fd5b505afa15801561140e573d6000803e3d6000fd5b505050506040513d602081101561142457600080fd5b5051600280546001600160a01b0319166001600160a01b039283161790556006546040805163836c081d60e01b81529051919092169163836c081d916004808301926020929190829003018186803b15801561147f57600080fd5b505afa158015611493573d6000803e3d6000fd5b505050506040513d60208110156114a957600080fd5b5051600580546001600160a01b03199081166001600160a01b03938416179091556003805482168984161790556004879055600780549091169186169190911790556114f482610d8f565b600080546001600160a01b0319166001600160a01b038516179055611518816110ab565b50505050505050565b6005546001600160a01b031681565b60008282018381101561158a576040805162461bcd60e51b815260206004820152601b60248201527f536166654d6174683a206164646974696f6e206f766572666c6f770000000000604482015290519081900360640190fd5b90505b92915050565b600061158a83836040518060400160405280601e81526020017f536166654d6174683a207375627472616374696f6e206f766572666c6f7700008152506116dc565b6000826115e45750600061158d565b828202828482816115f157fe5b041461158a5760405162461bcd60e51b815260040180806020018281038252602181526020018061182b6021913960400191505060405180910390fd5b600061158a83836040518060400160405280601a81526020017f536166654d6174683a206469766973696f6e206279207a65726f000000000000815250611773565b60008061167c856117d8565b8051602091820120604080516001600160f81b0319818501526bffffffffffffffffffffffff19606089901b1660218201526035810187905260558082019390935281518082039093018352607501905280519101209150509392505050565b6000818484111561176b5760405162461bcd60e51b81526004018080602001828103825283818151815260200191508051906020019080838360005b83811015611730578181015183820152602001611718565b50505050905090810190601f16801561175d5780820380516001836020036101000a031916815260200191505b509250505060405180910390fd5b505050900390565b600081836117c25760405162461bcd60e51b8152602060048201818152835160248401528351909283926044909101919085019080838360008315611730578181015183820152602001611718565b5060008385816117ce57fe5b0495945050505050565b604080516057810190915260378152733d602d80600a3d3981f3363d3d373d3d3d363d7360601b602082015260609190911b60348201526e5af43d82803e903d91602b57fd5bf360881b60488201529056fe536166654d6174683a206d756c7469706c69636174696f6e206f766572666c6f77a2646970667358221220adfbfc571b37e334a9832e0b7856a93889be67ce65a4ea75fea299374aac4b7164736f6c63430006060033";

    public static final String FUNC_ADMINFEEFRACTION = "adminFeeFraction";

    public static final String FUNC_AUTOSENDADMINFEE = "autoSendAdminFee";

    public static final String FUNC_CLAIMOWNERSHIP = "claimOwnership";

    public static final String FUNC_MIGRATIONMANAGER = "migrationManager";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PENDINGOWNER = "pendingOwner";

    public static final String FUNC_SIDECHAINDUFACTORY = "sidechainDUFactory";

    public static final String FUNC_SIDECHAINDUTEMPLATE = "sidechainDUTemplate";

    public static final String FUNC_SIDECHAINMAXGAS = "sidechainMaxGas";

    public static final String FUNC_TOKEN = "token";

    public static final String FUNC_TOKENMEDIATOR = "tokenMediator";

    public static final String FUNC_TOKENSSENTTOBRIDGE = "tokensSentToBridge";

    public static final String FUNC_TOTALADMINFEES = "totalAdminFees";

    public static final String FUNC_TOTALADMINFEESWITHDRAWN = "totalAdminFeesWithdrawn";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final String FUNC_VERSION = "version";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISINITIALIZED = "isInitialized";

    public static final String FUNC_AMB = "amb";

    public static final String FUNC_SETADMINFEE = "setAdminFee";

    public static final String FUNC_SETAUTOSENDADMINFEE = "setAutoSendAdminFee";

    public static final String FUNC_DEPLOYNEWDUSIDECHAIN = "deployNewDUSidechain";

    public static final String FUNC_SIDECHAINADDRESS = "sidechainAddress";

    public static final String FUNC_ONTOKENTRANSFER = "onTokenTransfer";

    public static final String FUNC_ONPURCHASE = "onPurchase";

    public static final String FUNC_ADMINFEESWITHDRAWABLE = "adminFeesWithdrawable";

    public static final String FUNC_UNACCOUNTEDTOKENS = "unaccountedTokens";

    public static final String FUNC_SENDTOKENSTOBRIDGE = "sendTokensToBridge";

    public static final String FUNC_WITHDRAWADMINFEES = "withdrawAdminFees";

    public static final String FUNC_MIGRATE = "migrate";

    public static final Event ADMINFEECHANGED_EVENT = new Event("AdminFeeChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event ADMINFEECHARGED_EVENT = new Event("AdminFeeCharged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event ADMINFEESWITHDRAWN_EVENT = new Event("AdminFeesWithdrawn", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event MIGRATEMEDIATOR_EVENT = new Event("MigrateMediator", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event MIGRATETOKEN_EVENT = new Event("MigrateToken", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
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

    public List<MigrateMediatorEventResponse> getMigrateMediatorEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(MIGRATEMEDIATOR_EVENT, transactionReceipt);
        ArrayList<MigrateMediatorEventResponse> responses = new ArrayList<MigrateMediatorEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MigrateMediatorEventResponse typedResponse = new MigrateMediatorEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newMediator = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.oldMediator = (Address) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<MigrateMediatorEventResponse> migrateMediatorEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, MigrateMediatorEventResponse>() {
            @Override
            public MigrateMediatorEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(MIGRATEMEDIATOR_EVENT, log);
                MigrateMediatorEventResponse typedResponse = new MigrateMediatorEventResponse();
                typedResponse.log = log;
                typedResponse.newMediator = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.oldMediator = (Address) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public Flowable<MigrateMediatorEventResponse> migrateMediatorEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MIGRATEMEDIATOR_EVENT));
        return migrateMediatorEventFlowable(filter);
    }

    public List<MigrateTokenEventResponse> getMigrateTokenEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(MIGRATETOKEN_EVENT, transactionReceipt);
        ArrayList<MigrateTokenEventResponse> responses = new ArrayList<MigrateTokenEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MigrateTokenEventResponse typedResponse = new MigrateTokenEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newToken = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.oldToken = (Address) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<MigrateTokenEventResponse> migrateTokenEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, MigrateTokenEventResponse>() {
            @Override
            public MigrateTokenEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(MIGRATETOKEN_EVENT, log);
                MigrateTokenEventResponse typedResponse = new MigrateTokenEventResponse();
                typedResponse.log = log;
                typedResponse.newToken = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.oldToken = (Address) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public Flowable<MigrateTokenEventResponse> migrateTokenEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MIGRATETOKEN_EVENT));
        return migrateTokenEventFlowable(filter);
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

    public RemoteFunctionCall<Address> migrationManager() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_MIGRATIONMANAGER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
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

    public RemoteFunctionCall<Address> sidechainDUFactory() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINDUFACTORY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> sidechainDUTemplate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SIDECHAINDUTEMPLATE, 
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

    public RemoteFunctionCall<Address> token() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKEN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> tokenMediator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKENMEDIATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> tokensSentToBridge() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TOKENSSENTTOBRIDGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
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

    public RemoteFunctionCall<TransactionReceipt> initialize(Address _migrationManager, Address _sidechainDUFactory, Uint256 _sidechainMaxGas, Address _sidechainDUTemplate, Address _owner, Uint256 _adminFeeFraction, DynamicArray<Address> agents) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(_migrationManager, _sidechainDUFactory, _sidechainMaxGas, _sidechainDUTemplate, _owner, _adminFeeFraction, agents), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Bool> isInitialized() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISINITIALIZED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> amb() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_AMB, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
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

    public RemoteFunctionCall<TransactionReceipt> onTokenTransfer(Address param0, Uint256 param1, DynamicBytes param2) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONTOKENTRANSFER, 
                Arrays.<Type>asList(param0, param1, param2), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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

    public RemoteFunctionCall<TransactionReceipt> migrate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_MIGRATE, 
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

    public static class MigrateMediatorEventResponse extends BaseEventResponse {
        public Address newMediator;

        public Address oldMediator;
    }

    public static class MigrateTokenEventResponse extends BaseEventResponse {
        public Address newToken;

        public Address oldToken;
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public Address previousOwner;

        public Address newOwner;
    }

    public static class RevenueReceivedEventResponse extends BaseEventResponse {
        public Uint256 amount;
    }
}
