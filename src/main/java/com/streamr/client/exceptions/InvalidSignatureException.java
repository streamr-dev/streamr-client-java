package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(StreamMessage msg) {
        super("Invalid signature for message: "+msg.toJson());
    }
}
