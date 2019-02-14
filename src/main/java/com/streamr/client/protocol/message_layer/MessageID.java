package com.streamr.client.protocol.message_layer;
import java.util.Date;

public class MessageID {
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private long sequenceNumber;
    private String publisherId;
    private String msgChainId;

    public MessageID(String streamId, int streamPartition, long timestamp, long sequenceNumber, String publisherId, String msgChainId) {
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
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

    public String getMsgChainId() {
        return msgChainId;
    }
}
