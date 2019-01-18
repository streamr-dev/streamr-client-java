package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

public class MessageID {
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private int sequenceNumber;
    private String publisherId;

    public MessageID(String streamId, int streamPartition, long timestamp, int sequenceNumber, String publisherId) {
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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getPublisherId() {
        return publisherId;
    }

    protected void writeJson(JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.value(streamId);
        writer.value(streamPartition);
        writer.value(timestamp);
        writer.value(sequenceNumber);
        writer.value(publisherId);
        writer.endArray();
    }
}