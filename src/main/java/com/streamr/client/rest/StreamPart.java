package com.streamr.client.rest;

public final class StreamPart {
  private final String streamId;
  private final int streamPartition;

  public StreamPart(final String streamId, final int streamPartition) {
    this.streamId = streamId;
    this.streamPartition = streamPartition;
  }

  public String getStreamId() {
    return this.streamId;
  }

  public int getStreamPartition() {
    return this.streamPartition;
  }
}
