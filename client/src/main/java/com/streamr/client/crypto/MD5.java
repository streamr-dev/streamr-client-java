package com.streamr.client.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MD5 {
  private MD5() {}

  public static byte[] digest(final byte[] b) {
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("MD5 digest is not available", e);
    }
    return md.digest(b);
  }

  public static byte[] digest(final String s) {
    return digest(s.getBytes(StandardCharsets.UTF_8));
  }
}
