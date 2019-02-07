package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class UnicastMessage extends ControlMessage {
    public static final int TYPE = 1;

    private final String subId;
    private final StreamMessage msg;

    public UnicastMessage(String subId, StreamMessage msg) {
        super(TYPE);
        this.subId = subId;
        this.msg = msg;
    }

    public String getSubId() {
        return subId;
    }

    public StreamMessage getStreamMessage() {
        return msg;
    }
}
