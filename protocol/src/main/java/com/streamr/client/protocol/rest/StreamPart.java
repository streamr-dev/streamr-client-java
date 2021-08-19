package com.streamr.client.protocol.rest;

import com.streamr.client.protocol.java.util.Objects;

public final class StreamPart {
  private final String streamId;
  private final int streamPartition;

  public StreamPart(final String streamId, final int streamPartition) {
    Objects.nonNull(streamId);
    this.streamId = streamId;
    this.streamPartition = streamPartition;
  }

  public String getStreamId() {
    return this.streamId;
  }

  public int getStreamPartition() {
    return this.streamPartition;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final StreamPart that = (StreamPart) o;
    return streamPartition == that.streamPartition && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamId, streamPartition);
  }

  @Override
  public String toString() {
    return String.format(
        "StreamPart{streamId='%s', streamPartition=%d}", streamId, streamPartition);
  }
}
