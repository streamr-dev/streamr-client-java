package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.*;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DataUnionJoinRequestTest {
  @Test
  void equalsHashcodeContract() {
    EqualsVerifier.forClass(DataUnionJoinRequest.class).verify();
  }
}
