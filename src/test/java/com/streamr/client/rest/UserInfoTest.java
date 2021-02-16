package com.streamr.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class UserInfoTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(UserInfo.class).verify();
  }
}
