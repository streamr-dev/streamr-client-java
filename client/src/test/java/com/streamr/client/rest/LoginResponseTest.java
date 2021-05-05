package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LoginResponseTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(LoginResponse.class).verify();
  }
}
