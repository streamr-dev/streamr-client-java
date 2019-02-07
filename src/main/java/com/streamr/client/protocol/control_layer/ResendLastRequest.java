package com.streamr.client.protocol.control_layer;

public class ResendLastRequest extends ControlMessage {
    public static final int TYPE = 11;

    private final String streamId;
    private final int streamPartition;
    private final String subId;
    private final int numberLast;

    public ResendLastRequest(String streamId, int streamPartition, String subId, int numberLast) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.subId = subId;
        this.numberLast = numberLast;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public String getSubId() {
        return subId;
    }

    public int getNumberLast() {
        return numberLast;
    }
}
