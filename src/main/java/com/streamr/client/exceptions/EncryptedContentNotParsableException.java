package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class EncryptedContentNotParsableException extends RuntimeException {
    public EncryptedContentNotParsableException(StreamMessage.EncryptionType encryptionType) {
        super("Content cannot be parsed since it is encrypted with " + encryptionType.getId() + ". Use serializedContent() method.");
    }
}
