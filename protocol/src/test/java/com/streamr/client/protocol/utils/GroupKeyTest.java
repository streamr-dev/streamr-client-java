package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import java.security.SecureRandom;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

class GroupKeyTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(GroupKey.class).verify();
  }

  @Test
  void validateGroupKeyThrowsOnInvalidKeyLength() {
    byte[] keyBytes = new byte[30];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(keyBytes);
    Exception e =
        assertThrows(
            InvalidGroupKeyException.class,
            () -> {
              GroupKey.validateGroupKey(Numeric.toHexStringNoPrefix(keyBytes));
            });

    assertEquals(
        "Group key must be 256 bits long, but got a key length of " + (30 * 8) + " bits.",
        e.getMessage());
  }

  @Test
  void validateGroupKeyDoesNotThrowOnCorrectKeyLength() throws InvalidGroupKeyException {
    byte[] keyBytes = new byte[32];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(keyBytes);
    GroupKey.validateGroupKey(Numeric.toHexStringNoPrefix(keyBytes));
  }
}
