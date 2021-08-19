package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
    assertEquals(expected, address.toString());
    assertEquals(new Address(expected), address);
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
  void toChecksumAddress() {
    Address a = new Address("000000004E7928DB8674762A13441A160E365EAE");
    assertEquals("0x000000004E7928DB8674762a13441A160e365eAE", a.toChecksumAddress());
  }
}
