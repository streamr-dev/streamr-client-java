package com.streamr.client.dataunion;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

/** Streamr Ethereum transaction receipt. */
public class EthereumTransactionReceipt {
  final TransactionReceipt tr;

  EthereumTransactionReceipt(final TransactionReceipt tr) {
    this.tr = tr;
  }

  public String getTransactionHash() {
    return tr.getTransactionHash();
  }
}
