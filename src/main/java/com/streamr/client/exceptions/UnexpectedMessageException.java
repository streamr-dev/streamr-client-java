package com.streamr.client.exceptions;

import com.streamr.client.protocol.control_layer.ControlMessage;

public class UnexpectedMessageException extends RuntimeException {
    public UnexpectedMessageException(ControlMessage msg) {
        super("Received unexpected control message: " + msg.toJson());
    }
}
