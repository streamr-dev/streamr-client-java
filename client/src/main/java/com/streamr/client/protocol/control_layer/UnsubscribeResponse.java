package com.streamr.client.protocol.control_layer;

public class UnsubscribeResponse extends ControlMessage {
    public static final int TYPE = 3;
    private String streamId;
    private int streamPartition;

    public UnsubscribeResponse(String requestId, String streamId, int streamPartition) {
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

    @Override
    public String toString() {
        return String.format("UnsubscribeResponse{requestId=%s, streamId=%s, streamPartition=%s",
                getRequestId(), streamId, streamPartition);
    }
}
