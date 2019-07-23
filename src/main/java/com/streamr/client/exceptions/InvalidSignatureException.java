package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class InvalidSignatureException extends RuntimeException {
    private boolean failedBecauseInvalidPublisher;
    public InvalidSignatureException(StreamMessage msg, Boolean validPublisher) {
        super("Invalid signature for message: "+msg.toJson());
        this.failedBecauseInvalidPublisher = validPublisher == null ? false : !validPublisher;
    }

    public boolean failedBecauseInvalidPublisher() {
        return failedBecauseInvalidPublisher;
    }
}
