package com.streamr.client.protocol.control_layer;

public class ResendResponseResent extends ResendResponse {
    public static final int TYPE = 5;

    public ResendResponseResent(String requestId, String streamId, int streamPartition) {
        super(TYPE, requestId, streamId, streamPartition);
    }
}
