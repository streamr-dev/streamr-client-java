package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PermissionTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Permission.class).verify();
  }
}
