package com.streamr.client.protocol.control_layer;

public abstract class ResendResponse extends ControlMessage {

    private String streamId;
    private int streamPartition;

    public ResendResponse(int type, String requestId, String streamId, int streamPartition) {
        super(type, requestId);
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
