package com.streamr.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(Address.class).verify();
  }
}
