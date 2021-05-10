package com.streamr.client.crypto;

public class KeysRsa {
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
