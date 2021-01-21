package com.streamr.client.protocol.message_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class MessageIdTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(MessageId.class).verify();
  }
}
