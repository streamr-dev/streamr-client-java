package com.streamr.client.protocol.common;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class MessageRefTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(MessageRef.class).verify();
  }
}
