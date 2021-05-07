package com.streamr.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.streamr.client.testing.TestingAddresses;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

class AddressTest {
  @Test
  void createsAddressWithGivenString() {
    final String input = "0x0000000000000000000000000000000000000001";
    Address address = new Address(input);
    assertEquals(input, address.toString());
  }

  @Test
  void createsAddressWithGivenByteArray() {
    final byte[] input = Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000001");
    Address address = new Address(input);
    assertEquals("0x0000000000000000000000000000000000000001", address.toString());
  }

  @Test
  void storesAddressInLowercase() {
    String input = "0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB1";
    final String expected = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1";
    Address address = new Address(input);
    assertEquals(expected, address.toLowerCaseString());
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Address.class).verify();
  }

  @Test
  void invalidAddressFormat() {
    try {
      new Address("foobar");
      fail("expecting IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
    try {
      new Address((String) null);
      fail("expecting IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  void createRandom() {
    Address a = TestingAddresses.createRandom();
    assertTrue(a.toString().startsWith("0x"));
    assertEquals(42, a.toString().length());
  }
}
