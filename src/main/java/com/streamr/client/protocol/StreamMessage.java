package com.streamr.client.protocol;

import java.util.Date;

public class StreamMessage {
    private String streamId;
    private int partition;
    private long timestamp;
    private Integer ttl;
    private Long offset;
    private Long previousOffset;
    private int contentTypeCode;
    private Object payload;

    public StreamMessage(String streamId, int partition, long timestamp, Integer ttl, Long offset, Long previousOffset, int contentTypeCode, Object payload) {
        this.streamId = streamId;
        this.partition = partition;
        this.timestamp = timestamp;
        this.ttl = ttl;
        this.offset = offset;
        this.previousOffset = previousOffset;
        this.contentTypeCode = contentTypeCode;
        this.payload = payload;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getPartition() {
        return partition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getTimestampAsDate() {
        return new Date(timestamp);
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

    public int getContentTypeCode() {
        return contentTypeCode;
    }

    public Object getPayload() {
        return payload;
    }
}
