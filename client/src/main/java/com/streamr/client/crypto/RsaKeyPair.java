package com.streamr.client.crypto;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RsaKeyPair implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(RsaKeyPair.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static KeyPairGenerator generator;
  private final RSAPublicKey rsaPublicKey;
  private final RSAPrivateKey rsaPrivateKey;

  static {
    try {
      generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(4096, SECURE_RANDOM);
    } catch (final NoSuchAlgorithmException e) {
      log.error("RSA key pair generator initialization error", e);
    }
  }

  public RsaKeyPair(final KeyPair keyPair) {
    this.rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
    this.rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
  }

  public static RsaKeyPair generateKeyPair() {
    return new RsaKeyPair(generator.generateKeyPair());
  }

  public RSAPublicKey getRsaPublicKey() {
    return rsaPublicKey;
  }

  public RSAPrivateKey getRsaPrivateKey() {
    return rsaPrivateKey;
  }
}
