package com.streamr.client.protocol.control_layer;

import com.streamr.client.protocol.message_layer.MessageRef;

public class ResendRangeRequest extends ControlMessage {
    public static final int TYPE = 13;

    private final String streamId;
    private final int streamPartition;
    private final String subId;
    private final MessageRef fromMsgRef;
    private final MessageRef toMsgRef;
    private final String publisherId;

    public ResendRangeRequest(String streamId, int streamPartition, String subId, MessageRef fromMsgRef, MessageRef toMsgRef, String publisherId) {
        super(TYPE);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.subId = subId;
        this.fromMsgRef = fromMsgRef;
        this.toMsgRef = toMsgRef;
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

    public MessageRef getToMsgRef() {
        return toMsgRef;
    }

    public String getPublisherId() {
        return publisherId;
    }
}
