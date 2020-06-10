package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendFromRequest;
import com.streamr.client.protocol.message_layer.MessageRef;
import java.util.Date;

public class ResendFromOption extends ResendOption {
    private MessageRef from;
    private String publisherId;
    private String msgChainId;

    public ResendFromOption(MessageRef from, String publisherId, String msgChainId) {
        this.from = from;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
    }

    public ResendFromOption(Date timestamp, long sequenceNumber, String publisherId, String msgChainId) {
        this(new MessageRef(timestamp.getTime(), sequenceNumber), publisherId, msgChainId);
    }

    public ResendFromOption(Date timestamp) {
        this(new MessageRef(timestamp.getTime(), 0L), null, null);
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
    public ControlMessage toRequest(String requestId, String streamId, int streamPartition, String sessionToken) {
        return new ResendFromRequest(requestId, streamId, streamPartition, from, publisherId, msgChainId, sessionToken);
    }
}
