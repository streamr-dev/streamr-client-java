package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.MessageRef;

public class ResendRangeRequest extends ControlMessage {
    public static final int TYPE = 13;

    private final String streamId;
    private final int streamPartition;
    private final MessageRef fromMsgRef;
    private final MessageRef toMsgRef;
    private final String publisherId;
    private final String msgChainId;
    private final String sessionToken;

    public ResendRangeRequest(String requestId, String streamId, int streamPartition, MessageRef fromMsgRef,
                              MessageRef toMsgRef, String sessionToken) {
        this(requestId, streamId, streamPartition, fromMsgRef, toMsgRef, null, null, sessionToken);
    }

    public ResendRangeRequest(String requestId, String streamId, int streamPartition, MessageRef fromMsgRef,
                              MessageRef toMsgRef, String publisherId, String msgChainId, String sessionToken) {
        super(TYPE, requestId);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.fromMsgRef = fromMsgRef;
        this.toMsgRef = toMsgRef;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
        this.sessionToken = sessionToken;

        if (fromMsgRef.compareTo(toMsgRef) > 0) {
            throw new IllegalArgumentException(String.format("fromMsgRef (%s) must be less than or equal to toMsgRef (%s)", fromMsgRef.toString(), toMsgRef.toString()));
        }
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

    public MessageRef getToMsgRef() {
        return toMsgRef;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getMsgChainId() {
        return msgChainId;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
