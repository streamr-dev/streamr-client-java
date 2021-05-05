package com.streamr.client.dataunion;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class EthereumTransactionReceiptTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(EthereumTransactionReceipt.class).verify();
  }
}
