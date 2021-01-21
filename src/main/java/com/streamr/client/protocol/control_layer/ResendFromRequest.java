package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.common.MessageRef;

public class ResendFromRequest extends ControlMessage {
    public static final int TYPE = 12;

    private final String streamId;
    private final int streamPartition;
    private final MessageRef fromMsgRef;
    private final String publisherId;
    private final String sessionToken;

    public ResendFromRequest(String requestId, String streamId, int streamPartition, MessageRef fromMsgRef, String sessionToken) {
        this(requestId, streamId, streamPartition, fromMsgRef, null, sessionToken);
    }

    public ResendFromRequest(String requestId, String streamId, int streamPartition, MessageRef fromMsgRef,
                             String publisherId, String sessionToken) {
        super(TYPE, requestId);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.fromMsgRef = fromMsgRef;
        this.publisherId = publisherId;
        this.sessionToken = sessionToken;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getStreamPartition() {
        return streamPartition;
    }

    public MessageRef getFromMsgRef() {
        return fromMsgRef;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    @Override
    public String toString() {
        return String.format("ResendFromRequest{requestId=%s, streamId=%s, streamPartition=%s, fromMsgRef=%s, publisherId=%s, sessionToken=%s",
                getRequestId(), streamId, streamPartition, fromMsgRef, publisherId, sessionToken);
    }
}
