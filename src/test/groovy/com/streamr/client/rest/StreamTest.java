package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class StreamTest {
  @Test
  void equalsContract() {
      EqualsVerifier.forClass(Stream.class).verify();
  }
}
