package com.streamr.client.protocol.message_layer;

import java.util.Date;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class MessageRef implements Comparable<MessageRef> {
  private final long timestamp;
  private final long sequenceNumber;

  public MessageRef(final long timestamp, final long sequenceNumber) {
    this.timestamp = timestamp;
    this.sequenceNumber = sequenceNumber;
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final MessageRef that = (MessageRef) o;
    return timestamp == that.timestamp && sequenceNumber == that.sequenceNumber;
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, sequenceNumber);
  }

  @Override
  public int compareTo(@NotNull MessageRef o) {
    if (timestamp < o.getTimestamp()) {
      return -1;
    } else if (timestamp > o.getTimestamp()) {
      return 1;
    }
    return (int) (sequenceNumber - o.sequenceNumber);
  }

  @Override
  public String toString() {
    return timestamp + "-" + sequenceNumber;
  }
}
