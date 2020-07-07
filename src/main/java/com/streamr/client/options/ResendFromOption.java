package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendFromRequest;
import com.streamr.client.protocol.message_layer.MessageRef;

import java.util.Date;

public class ResendFromOption extends ResendOption {
    private MessageRef from;
    private String publisherId;

    public ResendFromOption(MessageRef from, String publisherId) {
        this.from = from;
        this.publisherId = publisherId;
    }

    public ResendFromOption(Date timestamp, long sequenceNumber, String publisherId) {
        this(new MessageRef(timestamp.getTime(), sequenceNumber), publisherId);
    }

    public ResendFromOption(Date timestamp) {
        this(new MessageRef(timestamp.getTime(), 0L), null);
    }

    public MessageRef getFrom() {
        return from;
    }

    public String getPublisherId() {
        return publisherId;
    }

    @Override
    public ControlMessage toRequest(String requestId, String streamId, int streamPartition, String sessionToken) {
        return new ResendFromRequest(requestId, streamId, streamPartition, from, publisherId, sessionToken);
    }
}
