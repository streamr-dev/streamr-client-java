package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class SubscribersTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Subscribers.class).verify();
  }
}
