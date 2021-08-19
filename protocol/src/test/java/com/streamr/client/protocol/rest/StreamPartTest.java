package com.streamr.client.protocol.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class StreamPartTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(StreamPart.class).verify();
  }
}
