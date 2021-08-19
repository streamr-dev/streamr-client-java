package com.streamr.client.protocol.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RSAKeyPair {
  private static final SecureRandom SRAND = new SecureRandom();
  private static final Logger log = LoggerFactory.getLogger(RSAKeyPair.class);

  private final java.security.KeyPair keyPair;

  private RSAKeyPair(final KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  public RSAKeyPair(final RSAPublicKey rsaPublicKey, final RSAPrivateKey rsaPrivateKey) {
    this.keyPair = new KeyPair(rsaPublicKey, rsaPrivateKey);
  }

  public RSAPrivateKey getPrivateKey() {
    return (RSAPrivateKey) this.keyPair.getPrivate();
  }

  public RSAPublicKey getPublicKey() {
    return (RSAPublicKey) this.keyPair.getPublic();
  }

  public static RSAKeyPair create() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(4096, SRAND);
      KeyPair keyPair = generator.generateKeyPair();
      return new RSAKeyPair(keyPair);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String publicKeyToPem() {
    StringWriter writer = new StringWriter();
    PemWriter pemWriter = new PemWriter(writer);
    try {
      pemWriter.writeObject(new PemObject("PUBLIC KEY", this.keyPair.getPublic().getEncoded()));
      pemWriter.flush();
    } catch (IOException e) {
      String msg = "Failed to write key as PEM";
      log.error(msg, e);
      throw new RuntimeException(msg, e);
    } finally {
      try {
        pemWriter.close();
      } catch (IOException e) {
        log.error("Failed to close PemWriter", e);
      }
    }
    return writer.toString();
  }
}
