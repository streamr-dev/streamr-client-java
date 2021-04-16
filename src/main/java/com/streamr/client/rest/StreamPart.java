package com.streamr.client.rest;

import java.util.Objects;

public class StreamPart {

    String streamId;
    int streamPartition;

    public StreamPart(String streamId, int streamPartition) {
        this.streamId = streamId;
        this.streamPartition = streamPartition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamPart that = (StreamPart) o;
        return streamPartition == that.streamPartition && streamId.equals(that.streamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamId, streamPartition);
    }

    public String getStreamId() {
        return this.streamId;
    }

    public int getStreamPartition() {
        return this.streamPartition;
    }
}