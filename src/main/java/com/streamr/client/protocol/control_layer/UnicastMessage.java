package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class UnicastMessage extends ControlMessage {
    public static final int TYPE = 1;

    private final StreamMessage msg;

    public UnicastMessage(String requestId, StreamMessage msg) {
        super(TYPE, requestId);
        this.msg = msg;
    }

    public StreamMessage getStreamMessage() {
        return msg;
    }
}
