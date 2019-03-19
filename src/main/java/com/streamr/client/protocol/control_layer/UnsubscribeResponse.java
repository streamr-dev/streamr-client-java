package com.streamr.client.protocol.control_layer;

public class UnsubscribeResponse extends ControlMessage {
    public static final int TYPE = 3;
    private String streamId;
    private int streamPartition;

    public UnsubscribeResponse(String streamId, int streamPartition) {
        super(TYPE);
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
