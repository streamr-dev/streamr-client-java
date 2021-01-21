package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.Address;
import java.util.Date;
import java.util.Objects;

public final class MessageID {
  private final String streamId;
  private final int streamPartition;
  private final long timestamp;
  private final long sequenceNumber;
  private final Address publisherId;
  private final String msgChainId;

  public MessageID(
      String streamId,
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
    final MessageID messageID = (MessageID) o;
    return streamPartition == messageID.streamPartition
        && timestamp == messageID.timestamp
        && sequenceNumber == messageID.sequenceNumber
        && Objects.equals(streamId, messageID.streamId)
        && Objects.equals(publisherId, messageID.publisherId)
        && Objects.equals(msgChainId, messageID.msgChainId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
  }

  @Override
  public String toString() {
    return String.format(
        "MessageID{streamId='%s', streamPartition=%d, timestamp=%d, sequenceNumber=%d, publisherId='%s', msgChainId='%s'}",
        streamId, streamPartition, timestamp, sequenceNumber, publisherId, msgChainId);
  }
}
