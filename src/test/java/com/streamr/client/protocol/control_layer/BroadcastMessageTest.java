package com.streamr.client.protocol.control_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class BroadcastMessageTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(BroadcastMessage.class).usingGetClass().verify();
  }
}
