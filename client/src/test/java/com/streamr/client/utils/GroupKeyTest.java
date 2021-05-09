package com.streamr.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import java.security.SecureRandom;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

class GroupKeyTest {
  @Test
  void equalsContract() {
    EqualsVerifier.forClass(GroupKey.class).verify();
  }

  private String key(int bytes) {
    byte[] keyBytes = new byte[bytes];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(keyBytes);
    return Numeric.toHexStringNoPrefix(keyBytes);
  }

  @Test
  void validateGroupKeyThrowsOnInvalidLKeylength() {
    try {
      GroupKey.validate(key(32));
    } catch (InvalidGroupKeyException e) {
      String expected =
          "Group key must be 256 bits long, but got a key length of " + (30 * 8) + " bits.";
      assertEquals(expected, e.getMessage());
    }
  }

  @Test
  void validateGroupKeyDoesNotThrowOnCorrectKeyLength() throws InvalidGroupKeyException {
    GroupKey.validate(key(32));
  }
}
