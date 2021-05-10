package com.streamr.client.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KeysRsaTest {
  @Test
  void validatePublicKeyValidFormat() {
    RsaKeyPair keyPair = RsaKeyPair.generateKeyPair();
    String publicKey = KeysRsa.exportPublicKeyAsPemString(keyPair.getRsaPublicKey());
    KeysRsa.validatePublicKey(publicKey);
  }

  @Test
  void validatePublicKeyThrowsOnWrongFormat() {
    try {
      KeysRsa.validatePublicKey("wrong-format");
    } catch (IllegalArgumentException e) {
      assertEquals("Must be a valid RSA public key in the PEM format.", e.getMessage());
    }
  }

  @Test
  void validatePrivateKeyValidFormat() {
    RsaKeyPair keyPair = RsaKeyPair.generateKeyPair();
    String privateKey = KeysRsa.exportPrivateKeyAsPemString(keyPair.getRsaPrivateKey());
    KeysRsa.validatePrivateKey(privateKey);
  }

  @Test
  void validatePrivateKeyThrowsOnWrongFormat() {
    try {
      KeysRsa.validatePrivateKey("wrong-format");
    } catch (IllegalArgumentException e) {
      assertEquals("Must be a valid RSA private key in the PEM format.", e.getMessage());
    }
  }
}
