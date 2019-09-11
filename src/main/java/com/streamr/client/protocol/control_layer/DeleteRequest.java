package com.streamr.client.protocol.control_layer;

import java.util.Date;

public class DeleteRequest extends ControlMessage {
    public static final int TYPE = 14;

    private final String streamId;
    private final int streamPartition;
    // Used to link the received DeleteResponse to the DeleteRequest sent
    private final String requestId;
    private final Long fromTimestamp;
    private final Long toTimestamp;

    public DeleteRequest(String streamId, int streamPartition, String requestId, Long fromTimestamp, Long toTimestamp) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.requestId = requestId;
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
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

    public Long getFromTimestamp() {
        return fromTimestamp;
    }

    public Long getToTimestamp() {
        return toTimestamp;
    }

    public String getDeletionMessage() {
        String msg;
        if (fromTimestamp == null && toTimestamp == null) {
            msg = "all data";
        } else if (toTimestamp == null) {
            msg = "data from " + new Date(fromTimestamp);
        } else {
            msg = "data between " + new Date(fromTimestamp) + " and " + new Date(toTimestamp);
        }
        return msg + " for stream id " + streamId + " and partition " + streamPartition;
    }
}
