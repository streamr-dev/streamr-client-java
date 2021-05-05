package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ChallengeTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Challenge.class).verify();
  }
}
