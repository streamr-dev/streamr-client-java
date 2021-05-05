package com.streamr.client.java.util;

public final class Objects {
  private Objects() {
    throw new AssertionError("No com.streamr.client.java.util.Objects instances for you!");
  }

  public static <T> T requireNonNull(final T obj) {
    return java.util.Objects.<T>requireNonNull(obj);
  }

  public static <T> T requireNonNull(final T obj, final String message) {
    return java.util.Objects.<T>requireNonNull(obj, message);
  }

  public static boolean equals(final Object a, final Object b) {
    return java.util.Objects.equals(a, b);
  }

  public static int hash(final Object... values) {
    return java.util.Objects.hash(values);
  }

  public static boolean nonNull(final Object obj) {
    return java.util.Objects.nonNull(obj);
  }
}
