package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.Address;
import java.util.Date;
import java.util.Objects;

public final class MessageId {
  private final String streamId;
  private final int streamPartition;
  private final long timestamp;
  private final long sequenceNumber;
  private final Address publisherId;
  private final String msgChainId;

  private MessageId(
      final String streamId,
      final int streamPartition,
      final long timestamp,
      final long sequenceNumber,
      final Address publisherId,
      final String msgChainId) {
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
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final MessageId messageId = (MessageId) o;
    return streamPartition == messageId.streamPartition
        && timestamp == messageId.timestamp
        && sequenceNumber == messageId.sequenceNumber
        && Objects.equals(streamId, messageId.streamId)
        && Objects.equals(publisherId, messageId.publisherId)
        && Objects.equals(msgChainId, messageId.msgChainId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
  }

  @Override
  public String toString() {
    return String.format(
        "MessageId{streamId='%s', streamPartition=%d, timestamp=%d, sequenceNumber=%d, publisherId='%s', msgChainId='%s'}",
        streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
  }

  public final static class Builder {
    private String streamId;
    private int streamPartition;
    private long timestamp;
    private long sequenceNumber;
    private Address publisherId;
    private String msgChainId;

    public Builder() {}

    public Builder(final MessageId messageId) {
      this.streamId = messageId.streamId;
      this.streamPartition = messageId.streamPartition;
      this.timestamp = messageId.timestamp;
      this.sequenceNumber = messageId.sequenceNumber;
      this.publisherId = messageId.publisherId;
      this.msgChainId = messageId.msgChainId;
    }

    public Builder withStreamId(final String streamId) {
      this.streamId = streamId;
      return this;
    }

    public Builder withStreamPartition(final int streamPartition) {
      this.streamPartition = streamPartition;
      return this;
    }

    public Builder withTimestamp(final long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder withSequenceNumber(final long sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
      return this;
    }

    public Builder withPublisherId(final Address publisherId) {
      this.publisherId = publisherId;
      return this;
    }

    public Builder withMsgChainId(final String msgChainId) {
      this.msgChainId = msgChainId;
      return this;
    }

    public MessageId createMessageId() {
      return new MessageId(
          streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
    }
  }
}
