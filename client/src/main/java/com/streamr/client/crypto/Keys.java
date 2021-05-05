package com.streamr.client.crypto;

import java.math.BigInteger;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public final class Keys {
  private Keys() {}

  public static String privateKeyToAddressWithPrefix(final BigInteger privateKey) {
    final String address = privateKeyToAddressWithoutPrefix(privateKey);
    return Numeric.prependHexPrefix(address);
  }

  public static String privateKeyToAddressWithoutPrefix(final BigInteger privateKey) {
    final BigInteger publicKey = Sign.publicKeyFromPrivate(privateKey);
    final String address = org.web3j.crypto.Keys.getAddress(publicKey);
    return address;
  }
}
