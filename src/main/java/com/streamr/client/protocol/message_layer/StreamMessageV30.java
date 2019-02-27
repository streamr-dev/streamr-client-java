package com.streamr.client.protocol.message_layer;

import java.io.IOException;
import java.util.Map;

public class StreamMessageV30 extends StreamMessage {

    public static final int VERSION = 30;
    private MessageID messageID;
    private MessageRef previousMessageRef;
    private SignatureType signatureType;
    private String signature;

    public StreamMessageV30(MessageID messageID, MessageRef previousMessageRef, ContentType contentType,
                            String serializedContent, SignatureType signatureType, String signature) throws IOException {
        super(VERSION, contentType, serializedContent);
        this.messageID = messageID;
        this.previousMessageRef = previousMessageRef;
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV30(MessageID messageID, MessageRef previousMessageRef, ContentType contentType,
                            Map<String, Object> content, SignatureType signatureType, String signature) {
        super(VERSION, contentType, content);
        this.messageID = messageID;
        this.previousMessageRef = previousMessageRef;
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV30(String streamId, int streamPartition, long timestamp, long sequenceNumber,
                            String publisherId, String msgChainId, Long previousTimestamp, Long previousSequenceNumber, ContentType contentType,
                            String serializedContent, SignatureType signatureType, String signature) throws IOException {
        super(VERSION, contentType, serializedContent);
        this.messageID = new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
        if (previousTimestamp == null) {
            this.previousMessageRef = null;
        } else {
            this.previousMessageRef = new MessageRef(previousTimestamp, previousSequenceNumber);
        }
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV30(String streamId, int streamPartition, long timestamp, long sequenceNumber,
                            String publisherId, String msgChainId, Long previousTimestamp, Long previousSequenceNumber, ContentType contentType,
                            Map<String, Object> content, SignatureType signatureType, String signature) {
        super(VERSION, contentType, content);
        this.messageID = new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
        if (previousTimestamp == null) {
            this.previousMessageRef = null;
        } else {
            this.previousMessageRef = new MessageRef(previousTimestamp, previousSequenceNumber);
        }
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public MessageID getMessageID() {
        return messageID;
    }

    @Override
    public String getStreamId() {
        return messageID.getStreamId();
    }

    @Override
    public int getStreamPartition() {
        return messageID.getStreamPartition();
    }

    @Override
    public long getTimestamp() {
        return messageID.getTimestamp();
    }

    @Override
    public long getSequenceNumber() {
        return messageID.getSequenceNumber();
    }

    @Override
    public String getPublisherId() {
        return messageID.getPublisherId();
    }

    @Override
    public String getMsgChainId() {
        return messageID.getMsgChainId();
    }

    public MessageRef getPreviousMessageRef() {
        return previousMessageRef;
    }

    @Override
    public SignatureType getSignatureType() {
        return signatureType;
    }

    @Override
    public String getSignature() {
        return signature;
    }
}
