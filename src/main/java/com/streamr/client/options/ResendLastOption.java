package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendLastRequest;

public class ResendLastOption extends ResendOption {
    private int numberLast;

    public ResendLastOption(int numberLast) {
        this.numberLast = numberLast;
    }

    public int getNumberLast() {
        return numberLast;
    }

    @Override
    public ControlMessage toRequest(String streamId, int streamPartition, String subId, String sessionToken) {
        return new ResendLastRequest(streamId, streamPartition, subId, numberLast, sessionToken);
    }
}
