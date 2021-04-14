package com.streamr.client.rest;

public class StreamPart {

    String streamId;
    int streamPartition;

    public StreamPart(String streamId, int streamPartition) {
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