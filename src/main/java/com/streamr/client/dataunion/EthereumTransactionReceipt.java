package com.streamr.client.dataunion;

import java.util.Objects;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/** Streamr Ethereum transaction receipt. */
public final class EthereumTransactionReceipt {
  final TransactionReceipt tr;

  EthereumTransactionReceipt(final TransactionReceipt tr) {
    this.tr = tr;
  }

  public String getTransactionHash() {
    return tr.getTransactionHash();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EthereumTransactionReceipt that = (EthereumTransactionReceipt) o;
    return Objects.equals(tr, that.tr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tr);
  }
}
