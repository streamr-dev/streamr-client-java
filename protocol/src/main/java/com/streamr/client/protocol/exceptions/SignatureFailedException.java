package com.streamr.client.protocol.exceptions;

public class SignatureFailedException extends RuntimeException {
  public SignatureFailedException(String message) {
    super(message);
  }
}
