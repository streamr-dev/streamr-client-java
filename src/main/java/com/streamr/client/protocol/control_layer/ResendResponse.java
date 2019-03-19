package com.streamr.client.protocol.control_layer;

public abstract class ResendResponse extends ControlMessage {

    private String streamId;
    private int streamPartition;
    private String subId;

    public ResendResponse(int type, String streamId, int streamPartition, String subId) {
        super(type);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.subId = subId;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public String getSubId() {
        return subId;
    }
}
