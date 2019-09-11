package com.streamr.client.protocol.control_layer;

public class DeleteResponse extends ControlMessage {
    public static final int TYPE = 15;

    private final String streamId;
    private final int streamPartition;
    // the same as the requestId of the DeleteRequest that corresponds to this DeleteResponse
    private final String requestId;
    // true if the deletion was successful, false otherwise
    private final boolean status;

    public DeleteResponse(String streamId, int streamPartition, String requestId, boolean status) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.requestId = requestId;
        this.status = status;
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

    public boolean getStatus() {
        return status;
    }
}
