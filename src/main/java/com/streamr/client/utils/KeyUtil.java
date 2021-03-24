package com.streamr.client.utils;

import java.math.BigInteger;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public final class KeyUtil {
  private KeyUtil() {}

  public static String toHex(final BigInteger publicKey) {
    final String address = Keys.getAddress(publicKey);
    return Numeric.prependHexPrefix(address);
  }
}
