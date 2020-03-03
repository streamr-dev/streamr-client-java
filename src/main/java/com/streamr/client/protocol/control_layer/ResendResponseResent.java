package com.streamr.client.protocol.control_layer;

public class ResendResponseResent extends ResendResponse {
    public static final int TYPE = 5;

    public ResendResponseResent(String streamId, int streamPartition, String requestId) {
        super(TYPE, streamId, streamPartition, requestId);
    }
}
