package com.streamr.client.protocol.message_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ContentTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(StreamMessage.Content.class)
        .withIgnoredFields("adapter", "cache")
        .verify();
  }
}
