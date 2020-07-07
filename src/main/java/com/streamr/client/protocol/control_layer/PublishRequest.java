package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.StreamMessage;

public class PublishRequest extends ControlMessage {

    public static final int TYPE = 8;

    private final StreamMessage streamMessage;
    private final String sessionToken;

    public PublishRequest(String requestId, StreamMessage streamMessage, String sessionToken) {
        super(TYPE, requestId);
        this.streamMessage = streamMessage;
        this.sessionToken = sessionToken;
    }

    public StreamMessage getStreamMessage() {
        return streamMessage;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
