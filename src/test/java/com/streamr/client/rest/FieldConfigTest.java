package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class FieldConfigTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(FieldConfig.class).verify();
  }
}
