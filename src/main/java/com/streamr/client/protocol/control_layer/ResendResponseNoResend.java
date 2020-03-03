package com.streamr.client.protocol.control_layer;

public class ResendResponseNoResend extends ResendResponse {
    public static final int TYPE = 6;

    public ResendResponseNoResend(String streamId, int streamPartition, String requestId) {
        super(TYPE, streamId, streamPartition, requestId);
    }
}
