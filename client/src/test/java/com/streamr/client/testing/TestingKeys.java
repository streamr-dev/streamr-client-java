package com.streamr.client.testing;

import java.math.BigInteger;

public final class TestingKeys {
  public static final String CORE_API_PRIVATE_KEY =
      "0x0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

  private TestingKeys() {}

  public static BigInteger generatePrivateKey() {
    return new BigInteger("23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820", 16);
  }
}