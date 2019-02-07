package com.streamr.client.protocol.control_layer;

public class ResendResponseResending extends ControlMessage {
    public static final int TYPE = 4;

    private final String streamId;
    private final int streamPartition;
    private final String subId;

    public ResendResponseResending(String streamId, int streamPartition, String subId) {
        super(TYPE);
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
