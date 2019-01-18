package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

public class StreamMessageV28 extends StreamMessage {
    public static final int VERSION = 28;
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private Integer ttl;
    private Long offset;
    private Long previousOffset;

    public StreamMessageV28(String streamId, int streamPartition, long timestamp, Integer ttl, Long offset,
                            Long previousOffset, ContentType contentType, String serializedContent) throws IOException {
        super(VERSION, contentType, serializedContent);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.timestamp = timestamp;
        this.ttl = ttl;
        this.offset = offset;
        this.previousOffset = previousOffset;
    }

    @Override
    public String getStreamId() {
        return streamId;
    }

    @Override
    public int getStreamPartition() {
        return streamPartition;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }

    @Override
    public int getSequenceNumber() {
        return 0;
    }

    @Override
    public String getPublisherId() {
        return "";
    }

    public Integer getTtl() {
        return ttl;
    }

    public Long getOffset() {
        return offset;
    }

    public Long getPreviousOffset() {
        return previousOffset;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.value(streamId);
        writer.value(streamPartition);
        writer.value(timestamp);
        writer.value(ttl);
        writer.value(offset);
        writer.value(previousOffset);
        writer.value(contentType.getId());
        writer.value(serializedContent);
    }
}