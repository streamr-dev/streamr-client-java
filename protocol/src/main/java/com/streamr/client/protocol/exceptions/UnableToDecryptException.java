package com.streamr.client.protocol.exceptions;

public class UnableToDecryptException extends Exception {
  private UnableToDecryptException(final String message) {
    super(message);
  }

  public static UnableToDecryptException create(final String ciphertext) {
    String s = ciphertext;
    if (ciphertext.length() > 100) {
      s = ciphertext.substring(0, 100) + "...";
    }
    final String message = String.format("Unable to decrypt: %s", s);
    return new UnableToDecryptException(message);
  }
}
