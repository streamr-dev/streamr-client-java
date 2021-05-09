package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class ValidationException extends RuntimeException {
  private final Reason reason;
  private final StreamMessage streamMessage;

  public enum Reason {
    POLICY_VIOLATION,
    PERMISSION_VIOLATION,
    INVALID_SIGNATURE,
    INVALID_MESSAGE,
    UNSIGNED_NOT_ALLOWED
  }

  private ValidationException(
      final String message, final Reason reason, final StreamMessage streamMessage) {
    super(message);
    this.reason = reason;
    this.streamMessage = streamMessage;
  }

  public Reason getReason() {
    return reason;
  }

  public StreamMessage getStreamMessage() {
    return streamMessage;
  }

  public static class Factory {
    private static String formatStreamMessage(StreamMessage message) {
      if (message == null) {
        return "null";
      }
      return message.serialize();
    }

    public static ValidationException create(
        final String message, final Reason reason, final StreamMessage streamMessage) {
      final String msg =
          String.format(
              "Message validation failed due to: %s. StreamMessage was: %s",
              message, formatStreamMessage(streamMessage));
      return new ValidationException(msg, reason, streamMessage);
    }

    public static ValidationException create(
        final Reason reason, final StreamMessage streamMessage) {
      return new ValidationException(reason.toString(), reason, streamMessage);
    }
  }
}
