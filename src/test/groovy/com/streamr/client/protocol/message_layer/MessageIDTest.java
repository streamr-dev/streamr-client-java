package com.streamr.client.protocol.message_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class MessageIDTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(MessageID.class).verify();
  }
}
