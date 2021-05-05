package com.streamr.client.utils;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class GroupKeyTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(GroupKey.class).verify();
  }
}
