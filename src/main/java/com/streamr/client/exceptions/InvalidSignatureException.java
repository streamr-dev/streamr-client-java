package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class InvalidSignatureException extends ValidationException {
    private boolean failedBecauseInvalidPublisher;
    public InvalidSignatureException(StreamMessage msg, Boolean validPublisher) {
        super(msg, "Invalid signature. Valid publisher: " + validPublisher);
        this.failedBecauseInvalidPublisher = validPublisher == null ? false : !validPublisher;
    }

    public boolean failedBecauseInvalidPublisher() {
        return failedBecauseInvalidPublisher;
    }
}
