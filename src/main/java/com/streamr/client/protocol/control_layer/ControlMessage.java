package com.streamr.client.protocol.control_layer;

public abstract class ControlMessage {
    public static final int LATEST_VERSION = 1;
    private final int type;

    public ControlMessage(int type) {
        this.type = type;
    }
}
