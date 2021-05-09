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
    return org.web3j.crypto.Keys.getAddress(publicKey);
  }

  public static void validatePublicKey(String publicKey) {
    if (publicKey == null
        || !publicKey.startsWith("-----BEGIN PUBLIC KEY-----")
        || !publicKey.endsWith("-----END PUBLIC KEY-----\n")) {
      throw new IllegalArgumentException("Must be a valid RSA public key in the PEM format.");
    }
  }

  public static void validatePrivateKey(String privateKey) {
    if (privateKey == null
        || !privateKey.startsWith("-----BEGIN PRIVATE KEY-----")
        || !privateKey.endsWith("-----END PRIVATE KEY-----\n")) {
      throw new IllegalArgumentException("Must be a valid RSA private key in the PEM format.");
    }
  }
}
