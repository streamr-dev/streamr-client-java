package com.streamr.client.protocol.control_layer;

public class ResendResponseNoResend extends ControlMessage {
    public static final int TYPE = 6;

    private final String streamId;
    private final int streamPartition;
    private final String subId;

    public ResendResponseNoResend(String streamId, int streamPartition, String subId) {
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
