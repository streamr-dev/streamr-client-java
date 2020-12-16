package com.streamr.client.dataunion;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * wrapper to avoid exposing web3j TransactionReceipt
 */
public class EthereumTransactionReceipt {
    TransactionReceipt tr;
    EthereumTransactionReceipt(TransactionReceipt tr){
        this.tr = tr;
    }
    public String getTransactionHash(){
        return tr.getTransactionHash();
    }
}
