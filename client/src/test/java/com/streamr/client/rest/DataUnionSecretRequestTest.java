package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DataUnionSecretRequestTest {
  @Test
  void equalsHashcodeContract() {
    EqualsVerifier.forClass(DataUnionSecretRequest.class).verify();
  }
}
