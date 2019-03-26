package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;

public abstract class ResendOption {
    //The returned ControlMessage is either ResendLastRequest, ResendFromRequest or ResendRangeRequest
    public abstract ControlMessage toRequest(String streamId, int streamPartition, String subId, String sessionToken);
}
