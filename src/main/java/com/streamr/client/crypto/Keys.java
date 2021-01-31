package com.streamr.client.crypto;

import java.math.BigInteger;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public final class Keys {
  private Keys() {}

  public static String privateKeyToAddressWithPrefix(final BigInteger privateKey) {
    final BigInteger publicKey = Sign.publicKeyFromPrivate(privateKey);
    return Numeric.prependHexPrefix(org.web3j.crypto.Keys.getAddress(publicKey));
  }

  public static String privateKeyToAddressWithoutPrefix(final BigInteger privateKey) {
    final String address = privateKeyToAddressWithPrefix(privateKey);
    return address;
  }
}
