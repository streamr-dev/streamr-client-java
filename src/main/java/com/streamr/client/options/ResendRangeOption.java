package com.streamr.client.options;

import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import com.streamr.client.protocol.message_layer.MessageRef;

public class ResendRangeOption extends ResendOption {
    private MessageRef from;
    private MessageRef to;
    private String publisherId;
    private String msgChainId;

    public ResendRangeOption(long fromTimestamp, long fromSequenceNumber, long toTimestamp, long toSequenceNumber, String publisherId, String msgChainId) {
        from = new MessageRef(fromTimestamp, fromSequenceNumber);
        to = new MessageRef(toTimestamp, toSequenceNumber);
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
    }

    public ResendRangeOption(long fromTimestamp, long toTimestamp) {
        from = new MessageRef(fromTimestamp, 0L);
        to = new MessageRef(toTimestamp, 0L);
        this.publisherId = null;
        this.msgChainId = null;
    }

    @Override
    public ControlMessage toRequest(String streamId, int streamPartition, String subId, String sessionToken) {
        return new ResendRangeRequest(streamId, streamPartition, subId, from, to, publisherId, msgChainId, sessionToken);
    }
}
