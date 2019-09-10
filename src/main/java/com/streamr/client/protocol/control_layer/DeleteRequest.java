package com.streamr.client.protocol.control_layer;

public class DeleteRequest extends ControlMessage {
    public static final int TYPE = 14;

    private final String streamId;
    private final int streamPartition;
    private final Long fromTimestamp;
    private final Long toTimestamp;

    public DeleteRequest(String streamId, int streamPartition, Long fromTimestamp, Long toTimestamp) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public Long getFromTimestamp() {
        return fromTimestamp;
    }

    public Long getToTimestamp() {
        return toTimestamp;
    }
}
