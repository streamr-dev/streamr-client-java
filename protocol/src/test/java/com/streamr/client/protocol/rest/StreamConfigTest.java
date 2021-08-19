package com.streamr.client.protocol.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class StreamConfigTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(StreamConfig.class).verify();
  }
}
