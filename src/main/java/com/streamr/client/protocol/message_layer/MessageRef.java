package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

public class MessageRef {
    private Long timestamp;
    private int sequenceNumber;

    public MessageRef(Long timestamp, int sequenceNumber) {
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Date getTimestampAsDate() {
        if (timestamp == null) {
            return null;
        }
        return new Date(timestamp);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }


    protected void writeJson(JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.value(timestamp);
        writer.value(sequenceNumber);
        writer.endArray();
    }
}
