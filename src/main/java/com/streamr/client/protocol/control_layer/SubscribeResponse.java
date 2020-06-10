package com.streamr.client.protocol.control_layer;

public class SubscribeResponse extends ControlMessage {
    public static final int TYPE = 2;

    private final String streamId;
    private final int streamPartition;

    public SubscribeResponse(String requestId, String streamId, int streamPartition) {
        super(TYPE, requestId);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

}
