package com.streamr.client.protocol.control_layer;

public class DeleteResponse extends ControlMessage {
    public static final int TYPE = 15;

    private final String streamId;
    private final int streamPartition;
    // true if the deletion was successful, false otherwise
    private final boolean status;

    public DeleteResponse(String streamId, int streamPartition, boolean status) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.status = status;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public boolean getStatus() {
        return status;
    }
}
