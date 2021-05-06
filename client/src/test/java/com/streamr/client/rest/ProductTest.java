package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.*;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

final class ProductTest {
  @Test
  void equalsHashcodeContract() {
    EqualsVerifier.forClass(Product.class).usingGetClass().verify();
  }
}
