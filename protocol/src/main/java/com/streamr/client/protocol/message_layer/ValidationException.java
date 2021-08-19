package com.streamr.client.protocol.message_layer;

public class ValidationException extends RuntimeException {
  private final StreamMessage streamMessage;
  private final Reason reason;

  public enum Reason {
    POLICY_VIOLATION,
    PERMISSION_VIOLATION,
    INVALID_SIGNATURE,
    INVALID_MESSAGE,
    UNSIGNED_NOT_ALLOWED
  }

  public ValidationException(StreamMessage streamMessage, Reason reason, String message) {
    super(
        "Message validation failed due to: "
            + message
            + ". StreamMessage was: "
            + (streamMessage == null ? "null" : streamMessage.serialize()));
    this.reason = reason;
    this.streamMessage = streamMessage;
  }

  public ValidationException(StreamMessage streamMessage, Reason reason) {
    this(streamMessage, reason, reason.toString());
  }

  public Reason getReason() {
    return reason;
  }

  public StreamMessage getStreamMessage() {
    return streamMessage;
  }
}
