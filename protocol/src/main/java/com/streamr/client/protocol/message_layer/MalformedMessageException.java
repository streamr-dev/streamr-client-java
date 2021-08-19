package com.streamr.client.protocol.message_layer;

public class MalformedMessageException extends RuntimeException {
  public MalformedMessageException(String message) {
    super(message);
  }

  public MalformedMessageException(String message, Throwable cause) {
    super(message, cause);
  }
}
