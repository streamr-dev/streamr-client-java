package com.streamr.client.protocol.message_layer;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class MessageRef implements Comparable<MessageRef>{
    private long timestamp;
    private long sequenceNumber;

    public MessageRef(Long timestamp, long sequenceNumber) {
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

    @Override
    public int compareTo(@NotNull MessageRef o) {
        if (timestamp < o.getTimestamp()) {
            return -1;
        } else if (timestamp > o.getTimestamp()) {
            return 1;
        }
        return (int)(sequenceNumber - o.sequenceNumber);
    }

    @Override
    public String toString() {
        return timestamp+"-"+sequenceNumber;
    }
}
