package com.streamr.client.protocol.control_layer;

public class UnsubscribeRequest extends ControlMessage {
    public static final int TYPE = 10;

    private final String streamId;
    private final int streamPartition;

    public UnsubscribeRequest(String requestId, String streamId, int streamPartition) {
        super(TYPE, requestId);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
    }

    public String getStreamId() {
        return streamId;
    }

    public Integer getStreamPartition() {
        return streamPartition;
    }
}
