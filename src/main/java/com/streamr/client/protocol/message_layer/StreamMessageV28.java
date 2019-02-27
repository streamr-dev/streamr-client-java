package com.streamr.client.protocol.message_layer;

import java.io.IOException;
import java.util.Map;

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

    public StreamMessageV28(String streamId, int streamPartition, long timestamp, Integer ttl, Long offset,
                            Long previousOffset, ContentType contentType, Map<String, Object> content) {
        super(VERSION, contentType, content);
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
    public long getSequenceNumber() {
        return 0L;
    }

    @Override
    public String getPublisherId() {
        return "";
    }

    @Override
    public String getMsgChainId() {
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
    public SignatureType getSignatureType() {
        return SignatureType.SIGNATURE_TYPE_NONE;
    }

    @Override
    public String getSignature() {
        return null;
    }
}
