package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.MessageRef;

public class ResendFromRequest extends ControlMessage {
    public static final int TYPE = 12;

    private final String streamId;
    private final int streamPartition;
    private final String subId;
    private final MessageRef fromMsgRef;
    private final String publisherId;

    public ResendFromRequest(String streamId, int streamPartition, String subId, MessageRef fromMsgRef, String publisherId) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.subId = subId;
        this.fromMsgRef = fromMsgRef;
        this.publisherId = publisherId;
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

    public MessageRef getFromMsgRef() {
        return fromMsgRef;
    }

    public String getPublisherId() {
        return publisherId;
    }
}
