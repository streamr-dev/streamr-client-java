package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class ValidationException extends RuntimeException {

    public ValidationException(StreamMessage msg, String reason) {
        super("Validation of message failed: " + reason + ". Message was: " + (msg == null ? "null" : msg.toJson()));
    }

}
