package com.streamr.client.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.utils.EncryptionUtil;
import java.security.KeyPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KeysTest {
  @Test
  void validatePublicKeyValidFormat() {
    KeyPair keyPair = EncryptionUtil.generateKeyPair();
    String publicKey = EncryptionUtil.exportPublicKeyAsPemString(keyPair.getPublic());
    Keys.validatePublicKey(publicKey);
  }

  @Test
  void validatePublicKeyThrowsOnWrongFormat() {
    try {
      Keys.validatePublicKey("wrong-format");
    } catch (IllegalArgumentException e) {
      assertEquals("Must be a valid RSA public key in the PEM format.", e.getMessage());
    }
  }

  @Test
  void validatePrivateKeyValidFormat() {
    KeyPair keyPair = EncryptionUtil.generateKeyPair();
    String privateKey = EncryptionUtil.exportPrivateKeyAsPemString(keyPair.getPrivate());
    Keys.validatePrivateKey(privateKey);
  }

  @Test
  void validatePrivateKeyThrowsOnWrongFormat() {
    try {
      Keys.validatePrivateKey("wrong-format");
    } catch (IllegalArgumentException e) {
      assertEquals("Must be a valid RSA private key in the PEM format.", e.getMessage());
    }
  }
}