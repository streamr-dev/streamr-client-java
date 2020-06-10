package com.streamr.client.protocol.control_layer;

public class SubscribeRequest extends ControlMessage {
    public static final int TYPE = 9;

    private final String streamId;
    private final int streamPartition;
    private final String sessionToken;

    public SubscribeRequest(String requestId, String streamId, int streamPartition, String sessionToken) {
        super(TYPE, requestId);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.sessionToken = sessionToken;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
