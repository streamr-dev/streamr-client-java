package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class ContentTypeNotParsableException extends RuntimeException {
    public ContentTypeNotParsableException(StreamMessage.ContentType contentType) {
        super("Content with content type " + contentType.getId() + " cannot get parsed. Use serializedContent() method.");
    }
}
