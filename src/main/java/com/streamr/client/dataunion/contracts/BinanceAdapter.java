package com.streamr.client.dataunion.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
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
import org.web3j.tuples.generated.Tuple2;
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
public class BinanceAdapter extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50604051611005380380611005833981810160405260a081101561003357600080fd5b508051602082015160408301516060840151608090940151600280546001600160a01b039586166001600160a01b03199182161790915560008054948616948216949094179093556001805492851692841692909217909155600380549484169483169490941790935560048054929093169116179055610f4c806100b96000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c80638c30c933116100715780638c30c9331461026c57806395f9596a146102b55780639ccc42b8146102bd578063a4c0ed36146102c5578063c3059e9f1461035c578063f6f7923e14610382576100a9565b806311e07fd8146100ae57806343cd8f7e146100c85780635858aa26146100ec5780635fc8bf6f146101a55780638bb659ec146101ad575b600080fd5b6100b661038a565b60408051918252519081900360200190f35b6100d0610390565b604080516001600160a01b039092168252519081900360200190f35b6100d06004803603606081101561010257600080fd5b6001600160a01b0382351691602081013591810190606081016040820135600160201b81111561013157600080fd5b82018360208201111561014357600080fd5b803590602001918460018302840111600160201b8311171561016457600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092955061039f945050505050565b6100d0610558565b61026a600480360360608110156101c357600080fd5b6001600160a01b038235811692602081013590911691810190606081016040820135600160201b8111156101f657600080fd5b82018360208201111561020857600080fd5b803590602001918460018302840111600160201b8311171561022957600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610567945050505050565b005b6102926004803603602081101561028257600080fd5b50356001600160a01b0316610611565b604080516001600160a01b03909316835260208301919091528051918290030190f35b6100d0610636565b6100d0610645565b610348600480360360608110156102db57600080fd5b6001600160a01b0382351691602081013591810190606081016040820135600160201b81111561030a57600080fd5b82018360208201111561031c57600080fd5b803590602001918460018302840111600160201b8311171561033d57600080fd5b509092509050610654565b604080519115158252519081900360200190f35b61026a6004803603602081101561037257600080fd5b50356001600160a01b0316610733565b6100d0610740565b60055481565b6004546001600160a01b031681565b600081516041146103f7576040805162461bcd60e51b815260206004820152601860248201527f6572726f725f6261645369676e61747572654c656e6774680000000000000000604482015290519081900360640190fd5b60208201516040830151606084015160001a601b81101561041657601b015b8060ff16601b148061042b57508060ff16601c145b61047c576040805162461bcd60e51b815260206004820152601960248201527f6572726f725f6261645369676e617475726556657273696f6e00000000000000604482015290519081900360640190fd5b604080517f19457468657265756d205369676e6564204d6573736167653a0a37320000000060208083019190915260608a811b6bffffffffffffffffffffffff1916603c840152605083018a905230901b60708301528251808303606401815260848301808552815191830191909120600090915260a4830180855281905260ff851660c484015260e48301879052610104830186905292516001926101248082019392601f1981019281900390910190855afa158015610541573d6000803e3d6000fd5b5050604051601f1901519998505050505050505050565b6001546001600160a01b031681565b6001600160a01b03831660009081526006602052604081206001808201549192916105979163ffffffff61074f16565b9050846001600160a01b03166105ae85838661039f565b6001600160a01b0316146105f9576040805162461bcd60e51b815260206004820152600d60248201526c6261645f7369676e617475726560981b604482015290519081900360640190fd5b6001820181905561060a85856107b0565b5050505050565b600660205260009081526040902080546001909101546001600160a01b039091169082565b6000546001600160a01b031681565b6002546001600160a01b031681565b60008060001990506000600660006106a187878080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525061080c92505050565b6001600160a01b0390811682526020820192909252604001600020805490925016610709576040805162461bcd60e51b81526020600482015260136024820152721c9958da5c1a595b9d17dd5b9919599a5b9959606a1b604482015290519081900360640190fd5b8054600354610729916001600160a01b0390811691899116600186610870565b5050949350505050565b61073d33826107b0565b50565b6003546001600160a01b031681565b6000828201838110156107a9576040805162461bcd60e51b815260206004820152601b60248201527f536166654d6174683a206164646974696f6e206f766572666c6f770000000000604482015290519081900360640190fd5b9392505050565b6001600160a01b0382811660008181526006602052604080822080546001600160a01b03191694861694851781559051909392917ff12e3b42943f9288eaf8634459b4fad7424a14ac78268957d8e5f3a860d1f2b591a3505050565b600060148251101561085d576040805162461bcd60e51b8152602060048201526015602482015274746f416464726573735f6f75744f66426f756e647360581b604482015290519081900360640190fd5b506020810151600160601b90045b919050565b60025460009081906001600160a01b038681169116148061089857506001600160a01b038516155b1561096a57600254604080516370a0823160e01b815230600482015290516001600160a01b03909216935083916370a0823191602480820192602092909190829003018186803b1580156108eb57600080fd5b505afa1580156108ff573d6000803e3d6000fd5b505050506040513d602081101561091557600080fd5b5051905085811015610965576040805162461bcd60e51b8152602060048201526014602482015273696e73756666696369656e745f62616c616e636560601b604482015290519081900360640190fd5b610c36565b600254600080546040805163095ea7b360e01b81526001600160a01b039283166004820152602481018b90529051919093169263095ea7b39260448083019360209390929083900390910190829087803b1580156109c757600080fd5b505af11580156109db573d6000803e3d6000fd5b505050506040513d60208110156109f157600080fd5b5051610a35576040805162461bcd60e51b815260206004820152600e60248201526d185c1c1c9bdd9957d9985a5b195960921b604482015290519081900360640190fd5b6060610a4086610da4565b600080546040516338ed173960e01b8152600481018b8152602482018a90523060648301819052608483018a905260a060448401908152865160a485015286519697506001600160a01b03909416956338ed1739958e958d958a958e94909360c49092019160208089019202908190849084905b83811015610acc578181015183820152602001610ab4565b505050509050019650505050505050600060405180830381600087803b158015610af557600080fd5b505af1158015610b09573d6000803e3d6000fd5b505050506040513d6000823e601f3d908101601f191682016040526020811015610b3257600080fd5b8101908080516040519392919084600160201b821115610b5157600080fd5b908301906020820185811115610b6657600080fd5b82518660208202830111600160201b82111715610b8257600080fd5b82525081516020918201928201910280838360005b83811015610baf578181015183820152602001610b97565b505050509190910160408181526370a0823160e01b8252306004830152518c99506001600160a01b038a1696506370a082319550602480830195506020945090925090829003018186803b158015610c0657600080fd5b505afa158015610c1a573d6000803e3d6000fd5b505050506040513d6020811015610c3057600080fd5b50519150505b6001546001600160a01b0380841691634000aea0911683610c568b610ef2565b6040518463ffffffff1660e01b815260040180846001600160a01b03166001600160a01b0316815260200183815260200180602001828103825283818151815260200191508051906020019080838360005b83811015610cc0578181015183820152602001610ca8565b50505050905090810190601f168015610ced5780820380516001836020036101000a031916815260200191505b50945050505050602060405180830381600087803b158015610d0e57600080fd5b505af1158015610d22573d6000803e3d6000fd5b505050506040513d6020811015610d3857600080fd5b5050604080518781526020810183905281516001600160a01b03808b1693908616927f84484def420ee3a0d5f780231db9f0c3865ac023e847b17eed44379fd2ac1e02929081900390910190a3600554610d98908763ffffffff61074f16565b60055550505050505050565b6004546060906001600160a01b0316610e3e576040805160028082526060808301845292602083019080368337505060025482519293506001600160a01b031691839150600090610df157fe5b60200260200101906001600160a01b031690816001600160a01b0316815250508281600181518110610e1f57fe5b6001600160a01b0390921660209283029190910190910152905061086b565b60408051600380825260808201909252606091602082018380368337505060025482519293506001600160a01b031691839150600090610e7a57fe5b6001600160a01b039283166020918202929092010152600454825191169082906001908110610ea557fe5b60200260200101906001600160a01b031690816001600160a01b0316815250508281600281518110610ed357fe5b6001600160a01b03909216602092830291909101909101529050919050565b604080516001600160a01b0392909216600560a21b1860148301526034820190529056fea26469706673582212207ba652928fd71158dc443070a6f26af0fb142d30cff3f04f0ed526fe9841169964736f6c63430006060033";

    public static final String FUNC_BINANCERECIPIENT = "binanceRecipient";

    public static final String FUNC_BSCBRIDGE = "bscBridge";

    public static final String FUNC_CONVERTTOCOIN = "convertToCoin";

    public static final String FUNC_DATACOIN = "dataCoin";

    public static final String FUNC_DATACOINPASSED = "datacoinPassed";

    public static final String FUNC_HONEYSWAPROUTER = "honeyswapRouter";

    public static final String FUNC_LIQUIDITYTOKEN = "liquidityToken";

    public static final String FUNC_ONTOKENTRANSFER = "onTokenTransfer";

    public static final String FUNC_SETBINANCERECIPIENT = "setBinanceRecipient";

    public static final String FUNC_SETBINANCERECIPIENTFROMSIG = "setBinanceRecipientFromSig";

    public static final String FUNC_GETSIGNER = "getSigner";

    public static final Event SETBINANCERECIPIENT_EVENT = new Event("SetBinanceRecipient", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event WITHDRAWTOBINANCE_EVENT = new Event("WithdrawToBinance", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected BinanceAdapter(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected BinanceAdapter(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected BinanceAdapter(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected BinanceAdapter(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<SetBinanceRecipientEventResponse> getSetBinanceRecipientEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SETBINANCERECIPIENT_EVENT, transactionReceipt);
        ArrayList<SetBinanceRecipientEventResponse> responses = new ArrayList<SetBinanceRecipientEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SetBinanceRecipientEventResponse typedResponse = new SetBinanceRecipientEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.member = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.recipient = (Address) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SetBinanceRecipientEventResponse> setBinanceRecipientEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SetBinanceRecipientEventResponse>() {
            @Override
            public SetBinanceRecipientEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SETBINANCERECIPIENT_EVENT, log);
                SetBinanceRecipientEventResponse typedResponse = new SetBinanceRecipientEventResponse();
                typedResponse.log = log;
                typedResponse.member = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.recipient = (Address) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public Flowable<SetBinanceRecipientEventResponse> setBinanceRecipientEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SETBINANCERECIPIENT_EVENT));
        return setBinanceRecipientEventFlowable(filter);
    }

    public List<WithdrawToBinanceEventResponse> getWithdrawToBinanceEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(WITHDRAWTOBINANCE_EVENT, transactionReceipt);
        ArrayList<WithdrawToBinanceEventResponse> responses = new ArrayList<WithdrawToBinanceEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            WithdrawToBinanceEventResponse typedResponse = new WithdrawToBinanceEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.token = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.to = (Address) eventValues.getIndexedValues().get(1);
            typedResponse.amountDatacoin = (Uint256) eventValues.getNonIndexedValues().get(0);
            typedResponse.amountOtheroken = (Uint256) eventValues.getNonIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<WithdrawToBinanceEventResponse> withdrawToBinanceEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, WithdrawToBinanceEventResponse>() {
            @Override
            public WithdrawToBinanceEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(WITHDRAWTOBINANCE_EVENT, log);
                WithdrawToBinanceEventResponse typedResponse = new WithdrawToBinanceEventResponse();
                typedResponse.log = log;
                typedResponse.token = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.to = (Address) eventValues.getIndexedValues().get(1);
                typedResponse.amountDatacoin = (Uint256) eventValues.getNonIndexedValues().get(0);
                typedResponse.amountOtheroken = (Uint256) eventValues.getNonIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public Flowable<WithdrawToBinanceEventResponse> withdrawToBinanceEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(WITHDRAWTOBINANCE_EVENT));
        return withdrawToBinanceEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple2<Address, Uint256>> binanceRecipient(Address param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BINANCERECIPIENT, 
                Arrays.<Type>asList(param0), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple2<Address, Uint256>>(function,
                new Callable<Tuple2<Address, Uint256>>() {
                    @Override
                    public Tuple2<Address, Uint256> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<Address, Uint256>(
                                (Address) results.get(0), 
                                (Uint256) results.get(1));
                    }
                });
    }

    public RemoteFunctionCall<Address> bscBridge() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BSCBRIDGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> convertToCoin() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_CONVERTTOCOIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> dataCoin() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATACOIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Uint256> datacoinPassed() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DATACOINPASSED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> honeyswapRouter() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_HONEYSWAPROUTER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<Address> liquidityToken() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_LIQUIDITYTOKEN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    public RemoteFunctionCall<TransactionReceipt> onTokenTransfer(Address param0, Uint256 amount, DynamicBytes data) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ONTOKENTRANSFER, 
                Arrays.<Type>asList(param0, amount, data), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setBinanceRecipient(Address recipient) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETBINANCERECIPIENT, 
                Arrays.<Type>asList(recipient), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setBinanceRecipientFromSig(Address from, Address recipient, DynamicBytes sig) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETBINANCERECIPIENTFROMSIG, 
                Arrays.<Type>asList(from, recipient, sig), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Address> getSigner(Address recipient, Uint256 nonce, DynamicBytes signature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSIGNER, 
                Arrays.<Type>asList(recipient, nonce, signature), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function);
    }

    @Deprecated
    public static BinanceAdapter load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new BinanceAdapter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static BinanceAdapter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new BinanceAdapter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static BinanceAdapter load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new BinanceAdapter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static BinanceAdapter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new BinanceAdapter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<BinanceAdapter> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, Address dataCoin_, Address honeyswapRouter_, Address bscBridge_, Address convertToCoin_, Address liquidityToken_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(dataCoin_, honeyswapRouter_, bscBridge_, convertToCoin_, liquidityToken_));
        return deployRemoteCall(BinanceAdapter.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<BinanceAdapter> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, Address dataCoin_, Address honeyswapRouter_, Address bscBridge_, Address convertToCoin_, Address liquidityToken_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(dataCoin_, honeyswapRouter_, bscBridge_, convertToCoin_, liquidityToken_));
        return deployRemoteCall(BinanceAdapter.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<BinanceAdapter> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, Address dataCoin_, Address honeyswapRouter_, Address bscBridge_, Address convertToCoin_, Address liquidityToken_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(dataCoin_, honeyswapRouter_, bscBridge_, convertToCoin_, liquidityToken_));
        return deployRemoteCall(BinanceAdapter.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<BinanceAdapter> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, Address dataCoin_, Address honeyswapRouter_, Address bscBridge_, Address convertToCoin_, Address liquidityToken_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(dataCoin_, honeyswapRouter_, bscBridge_, convertToCoin_, liquidityToken_));
        return deployRemoteCall(BinanceAdapter.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class SetBinanceRecipientEventResponse extends BaseEventResponse {
        public Address member;

        public Address recipient;
    }

    public static class WithdrawToBinanceEventResponse extends BaseEventResponse {
        public Address token;

        public Address to;

        public Uint256 amountDatacoin;

        public Uint256 amountOtheroken;
    }
}
