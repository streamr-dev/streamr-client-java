package com.streamr.client.protocol.control_layer;

public abstract class ResendResponse extends ControlMessage {

    private String streamId;
    private int streamPartition;
    private String requestId;

    public ResendResponse(int type, String streamId, int streamPartition, String requestId) {
        super(type);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.requestId = requestId;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public String getRequestId() {
        return requestId;
    }
}
