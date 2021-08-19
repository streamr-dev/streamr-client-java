package com.streamr.client.testing;

public final class TestingStreams {
  private TestingStreams() {}

  public static String generateName(Class<?> tetsClassName) {
    return String.format("%s-%s", tetsClassName.getSimpleName(), System.currentTimeMillis());
  }

  public static String generateId(Class<?> tetsClassName) {
    return String.format("/%s/%s", tetsClassName.getSimpleName(), System.currentTimeMillis());
  }
}
