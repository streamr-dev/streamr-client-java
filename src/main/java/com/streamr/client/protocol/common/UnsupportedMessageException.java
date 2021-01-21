package com.streamr.client.protocol.common;

public class UnsupportedMessageException extends RuntimeException {
  public UnsupportedMessageException(final String message) {
    super(message);
  }
}
