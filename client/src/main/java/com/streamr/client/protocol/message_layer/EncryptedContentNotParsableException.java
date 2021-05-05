package com.streamr.client.protocol.message_layer;

public class EncryptedContentNotParsableException extends RuntimeException {
  public EncryptedContentNotParsableException(StreamMessage.EncryptionType encryptionType) {
    super(
        "Content cannot be parsed since it is encrypted with "
            + encryptionType.getId()
            + ". Use serializedContent() method.");
  }
}
