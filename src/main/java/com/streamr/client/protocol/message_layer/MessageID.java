package com.streamr.client.protocol.message_layer;
import java.util.Date;

public class MessageID {
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private long sequenceNumber;
    private String publisherId;

    public MessageID(String streamId, int streamPartition, long timestamp, long sequenceNumber, String publisherId) {
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.publisherId = publisherId;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPublisherId() {
        return publisherId;
    }
}
