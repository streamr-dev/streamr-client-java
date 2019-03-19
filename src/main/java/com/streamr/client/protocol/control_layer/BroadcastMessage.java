package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class BroadcastMessage extends ControlMessage {
    public static final int TYPE = 0;

    private final StreamMessage msg;

    public BroadcastMessage(StreamMessage msg) {
        super(TYPE);
        this.msg = msg;
    }

    public StreamMessage getStreamMessage() {
        return msg;
    }
}
