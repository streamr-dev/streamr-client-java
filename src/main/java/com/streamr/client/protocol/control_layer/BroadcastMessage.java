package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class BroadcastMessage extends ControlMessage {
    public static final int TYPE = 0;

    private final StreamMessage streamMessage;

    public BroadcastMessage(String requestId, StreamMessage streamMessage) {
        super(TYPE, requestId);
        this.streamMessage = streamMessage;
    }

    public StreamMessage getStreamMessage() {
        return streamMessage;
    }
}
