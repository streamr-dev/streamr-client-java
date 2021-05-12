package com.streamr.client.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public final class IdGenerator {
  private IdGenerator() {}

  /** Returns an URL-safe base64 encoding of a randomly generated UUID */
  public static String get() {
    UUID uuid = UUID.randomUUID();

    byte[] bytes = new byte[16];
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());

    return new String(Base64.getUrlEncoder().encode(bytes));
  }
}
