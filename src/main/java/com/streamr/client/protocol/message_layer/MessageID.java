package com.streamr.client.protocol.message_layer;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.utils.Address;

import java.util.Date;

public class MessageID {
    private final String streamId;
    private final int streamPartition;
    private final long timestamp;
    private final long sequenceNumber;
    private final Address publisherId;
    private final String msgChainId;

    public MessageID(String streamId, int streamPartition, long timestamp, long sequenceNumber, Address publisherId, String msgChainId) {
        if (streamId == null) {
            throw new MalformedMessageException("'streamId' cannot be null.");
        }
        if (publisherId == null) {
            throw new MalformedMessageException("'publisherId' cannot be null.");
        }
        if (msgChainId == null) {
            throw new MalformedMessageException("'msgChainId' cannot be null.");
        }
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
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

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Address getPublisherId() {
        return publisherId;
    }

    public String getMsgChainId() {
        return msgChainId;
    }

    @Override
    public String toString() {
        return "MessageID{" +
                "streamId='" + streamId + '\'' +
                ", streamPartition=" + streamPartition +
                ", timestamp=" + timestamp +
                ", sequenceNumber=" + sequenceNumber +
                ", publisherId='" + publisherId + '\'' +
                ", msgChainId='" + msgChainId + '\'' +
                '}';
    }
}
