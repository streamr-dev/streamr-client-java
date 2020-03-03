package com.streamr.client.protocol.control_layer;

public class ResendLastRequest extends ControlMessage {
    public static final int TYPE = 11;

    private final String streamId;
    private final int streamPartition;
    private final String requestId;
    private final int numberLast;
    private final String sessionToken;

    public ResendLastRequest(String streamId, int streamPartition, String requestId, int numberLast, String sessionToken) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.requestId = requestId;
        this.numberLast = numberLast;
        this.sessionToken = sessionToken;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getNumberLast() {
        return numberLast;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
