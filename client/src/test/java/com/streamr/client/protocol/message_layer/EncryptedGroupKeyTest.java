package com.streamr.client.protocol.message_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class EncryptedGroupKeyTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(EncryptedGroupKey.class).withIgnoredFields("serialized").verify();
  }
}
