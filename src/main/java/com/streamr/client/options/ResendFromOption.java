package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendFromRequest;
import com.streamr.client.protocol.message_layer.MessageRef;

public class ResendFromOption extends ResendOption {
    private MessageRef from;
    private String publisherId;
    private String msgChainId;

    public ResendFromOption(MessageRef from, String publisherId, String msgChainId) {
        this.from = from;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
    }

    public ResendFromOption(long timestamp, long sequenceNumber, String publisherId, String msgChainId) {
        this(new MessageRef(timestamp, sequenceNumber), publisherId, msgChainId);
    }

    public ResendFromOption(long timestamp) {
        this(new MessageRef(timestamp, 0L), null, null);
    }

    public MessageRef getFrom() {
        return from;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public String getMsgChainId() {
        return msgChainId;
    }

    @Override
    public ControlMessage toRequest(String streamId, int streamPartition, String subId, String sessionToken) {
        return new ResendFromRequest(streamId, streamPartition, subId, from, publisherId, msgChainId, sessionToken);
    }
}
