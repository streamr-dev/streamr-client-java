package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.Date;

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

    public StreamMessageV30(String streamId, int streamPartition, long timestamp, int sequenceNumber,
                            String publisherId, Long previousTimestamp, int previousSequenceNumber, ContentType contentType,
                            String serializedContent, SignatureType signatureType, String signature) throws IOException {
        super(VERSION, contentType, serializedContent);
        this.messageID = new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId);
        this.previousMessageRef = new MessageRef(previousTimestamp, previousSequenceNumber);
        this.signatureType = signatureType;
        this.signature = signature;
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
    public Date getTimestampAsDate() {
        return messageID.getTimestampAsDate();
    }

    @Override
    public int getSequenceNumber() {
        return messageID.getSequenceNumber();
    }

    @Override
    public String getPublisherId() {
        return messageID.getPublisherId();
    }

    public MessageRef getPreviousMessageRef() {
        return previousMessageRef;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        messageID.writeJson(writer);
        previousMessageRef.writeJson(writer);
        writer.value(contentType.getId());
        writer.value(serializedContent);
        writer.value(signatureType.getId());
        writer.value(signature);
    }

}
