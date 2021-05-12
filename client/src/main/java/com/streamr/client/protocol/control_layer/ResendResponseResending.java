package com.streamr.client.protocol.control_layer;

public class ResendResponseResending extends ResendResponse {
  public static final int TYPE = 4;

  public ResendResponseResending(String requestId, String streamId, int streamPartition) {
    super(TYPE, requestId, streamId, streamPartition);
  }
}
