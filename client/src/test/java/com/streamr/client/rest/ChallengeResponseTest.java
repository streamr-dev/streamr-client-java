package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ChallengeResponseTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(ChallengeResponse.class).verify();
  }
}
