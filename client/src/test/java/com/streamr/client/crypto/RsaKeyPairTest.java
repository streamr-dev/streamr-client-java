package com.streamr.client.crypto;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class RsaKeyPairTest {
  @Test
  void generatesRsaKeyPair() {
    RsaKeyPair rsaKeyPair = RsaKeyPair.generateKeyPair();
    assertNotNull(rsaKeyPair.getRsaPublicKey());
    assertNotNull(rsaKeyPair.getRsaPrivateKey());
  }
}
