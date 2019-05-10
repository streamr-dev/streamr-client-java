package com.streamr.client.protocol.message_layer;

import java.io.IOException;
import java.util.Map;

public class StreamMessageV29 extends StreamMessage {

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
        super(VERSION, contentType, EncryptionType.NONE, serializedContent);
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

    public StreamMessageV29(String streamId, int streamPartition, long timestamp, Integer ttl, Long offset,
                            Long previousOffset, ContentType contentType, Map<String, Object> content,
                            SignatureType signatureType, String publisherAddress, String signature) throws IOException {
        super(VERSION, contentType, EncryptionType.NONE, content);
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

    @Override
    public String getPublisherId() {
        return publisherAddress;
    }

    @Override
    public String getMsgChainId() {
        return "";
    }

    @Override
    public MessageRef getPreviousMessageRef() {
        return null;
    }

    @Override
    public SignatureType getSignatureType() {
        return signatureType;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignatureType(SignatureType signatureType) {
        throw new AbstractMethodError("This method is not implemented in version 29");
    }

    @Override
    public void setSignature(String signature) {
        throw new AbstractMethodError("This method is not implemented in version 29");
    }
}
