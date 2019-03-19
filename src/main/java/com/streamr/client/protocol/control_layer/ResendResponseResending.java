package com.streamr.client.protocol.control_layer;

public class ResendResponseResending extends ResendResponse {
    public static final int TYPE = 4;

    public ResendResponseResending(String streamId, int streamPartition, String subId) {
        super(TYPE, streamId, streamPartition, subId);
    }
}
