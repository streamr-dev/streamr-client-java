package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PublishersTest {
  @Test
  void equalsContract() {
      EqualsVerifier.forClass(Publishers.class).verify();
  }
}
