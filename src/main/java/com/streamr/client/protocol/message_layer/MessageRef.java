package com.streamr.client.protocol.message_layer;
import java.util.Date;

public class MessageRef {
    private long timestamp;
    private long sequenceNumber;

    public MessageRef(Long timestamp, int sequenceNumber) {
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
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
}
