package com.streamr.client.crypto;

import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KeysRsa {
  private static final Logger log = LoggerFactory.getLogger(KeysRsa.class);

  private KeysRsa() {}

  public static void validatePublicKey(final String publicKey) {
    if (publicKey == null
        || !publicKey.startsWith("-----BEGIN PUBLIC KEY-----")
        || !publicKey.endsWith("-----END PUBLIC KEY-----\n")) {
      throw new IllegalArgumentException("Must be a valid RSA public key in the PEM format.");
    }
  }

  public static void validatePrivateKey(final String privateKey) {
    if (privateKey == null
        || !privateKey.startsWith("-----BEGIN PRIVATE KEY-----")
        || !privateKey.endsWith("-----END PRIVATE KEY-----\n")) {
      throw new IllegalArgumentException("Must be a valid RSA private key in the PEM format.");
    }
  }

  public static String exportPublicKeyAsPemString(final Key key) {
    return exportKeyAsPemString(key, "PUBLIC");
  }

  public static String exportPrivateKeyAsPemString(final Key key) {
    return exportKeyAsPemString(key, "PRIVATE");
  }

  private static String exportKeyAsPemString(final Key key, final String visibility) {
    StringWriter writer = new StringWriter();
    PemWriter pemWriter = new PemWriter(writer);
    try {
      pemWriter.writeObject(new PemObject(visibility + " KEY", key.getEncoded()));
      pemWriter.flush();
    } catch (IOException e) {
      log.error("Failed to write key as PEM", e);
      throw new RuntimeException(e);
    } finally {
      try {
        pemWriter.close();
      } catch (IOException e) {
        log.error("Failed to close PemWriter", e);
      }
    }
    return writer.toString();
  }

  public static RSAPublicKey getPublicKeyFromString(String publicKey) {
    publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----\n", "");
    publicKey = publicKey.replace("-----END PUBLIC KEY-----", "");
    byte[] encoded = Base64.getMimeDecoder().decode(publicKey);
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
    } catch (Exception e) {
      log.error("Failed to parse public key from string: " + publicKey, e);
      throw new RuntimeException(e);
    }
  }

  public static RSAPrivateKey getPrivateKeyFromString(String privateKey) {
    privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----\n", "");
    privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
    byte[] encoded = Base64.getMimeDecoder().decode(privateKey);
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
      return (RSAPrivateKey) kf.generatePrivate(keySpec);
    } catch (Exception e) {
      log.error("Failed to parse private key", e);
      throw new RuntimeException(e);
    }
  }
}
