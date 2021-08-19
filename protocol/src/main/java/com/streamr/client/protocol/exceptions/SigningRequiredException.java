package com.streamr.client.protocol.exceptions;

public class SigningRequiredException extends RuntimeException {
  public SigningRequiredException(String message) {
    super(message);
  }
}
