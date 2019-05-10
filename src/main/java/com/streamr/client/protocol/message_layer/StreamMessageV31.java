package com.streamr.client.protocol.message_layer;

import java.util.Map;

public class StreamMessageV31 extends StreamMessage {

    public static final int VERSION = 31;
    private MessageID messageID;
    private MessageRef previousMessageRef;
    private SignatureType signatureType;
    private String signature;

    public StreamMessageV31(MessageID messageID, MessageRef previousMessageRef, ContentType contentType, EncryptionType encryptionType,
                            String serializedContent, SignatureType signatureType, String signature) {
        super(VERSION, contentType, encryptionType, serializedContent);
        this.messageID = messageID;
        this.previousMessageRef = previousMessageRef;
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV31(MessageID messageID, MessageRef previousMessageRef, ContentType contentType,
                            EncryptionType encryptionType, Map<String, Object> content,
                            SignatureType signatureType, String signature) {
        super(VERSION, contentType, encryptionType, content);
        this.messageID = messageID;
        this.previousMessageRef = previousMessageRef;
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV31(String streamId, int streamPartition, long timestamp, long sequenceNumber,
                            String publisherId, String msgChainId, Long previousTimestamp, Long previousSequenceNumber,
                            ContentType contentType, EncryptionType encryptionType,
                            String serializedContent, SignatureType signatureType, String signature) {
        super(VERSION, contentType, encryptionType, serializedContent);
        this.messageID = new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
        if (previousTimestamp == null) {
            this.previousMessageRef = null;
        } else {
            this.previousMessageRef = new MessageRef(previousTimestamp, previousSequenceNumber);
        }
        this.signatureType = signatureType;
        this.signature = signature;
    }

    public StreamMessageV31(String streamId, int streamPartition, long timestamp, long sequenceNumber,
                            String publisherId, String msgChainId, Long previousTimestamp, Long previousSequenceNumber,
                            ContentType contentType, EncryptionType encryptionType,
                            Map<String, Object> content, SignatureType signatureType, String signature) {
        super(VERSION, contentType, encryptionType, content);
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

    @Override
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

    @Override
    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
