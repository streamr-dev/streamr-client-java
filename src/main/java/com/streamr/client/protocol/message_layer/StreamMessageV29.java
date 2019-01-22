package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

public class StreamMessageV29 extends StreamMessage {

    private static final StreamMessageV29Adapter v29Adapter = new StreamMessageV29Adapter();

    public static final int VERSION = 29;
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private Integer ttl;
    private Long offset;
    private Long previousOffset;
    private SignatureType signatureType;
    private String publisherAddress;
    private String signature;

    public StreamMessageV29(String streamId, int streamPartition, long timestamp, Integer ttl, Long offset,
                            Long previousOffset, ContentType contentType, String serializedContent,
                            SignatureType signatureType, String publisherAddress, String signature) throws IOException {
        super(VERSION, contentType, serializedContent);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.timestamp = timestamp;
        this.ttl = ttl;
        this.offset = offset;
        this.previousOffset = previousOffset;
        this.signatureType = signatureType;
        this.publisherAddress = publisherAddress;
        this.signature = signature;
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
    public long getSequenceNumber() {
        return 0L;
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

    public SignatureType getSignatureType() {
        return signatureType;
    }

    @Override
    public String getPublisherId() {
        return publisherAddress;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        v29Adapter.toJson(writer, this);
    }
}
