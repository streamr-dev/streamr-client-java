package com.streamr.client.protocol.control_layer;

public class ResendResponseNoResend extends ResendResponse {
    public static final int TYPE = 6;

    public ResendResponseNoResend(String requestId, String streamId, int streamPartition) {
        super(TYPE, requestId, streamId, streamPartition);
    }
}
