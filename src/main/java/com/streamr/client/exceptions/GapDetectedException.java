package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.utils.Address;

public class GapDetectedException extends RuntimeException {
	private final String streamId;
	private final int streamPartition;
	private final MessageRef from;
	private final MessageRef to;
	private final Address publisherId;
	private final String msgChainId;

    public GapDetectedException(String streamId, int streamPartition,
                                MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
        super("Gap Detected for stream " + streamId + ", partition " + streamPartition
        	+ ", publisher " + publisherId + ", message chain " + msgChainId + " between " + from + " and " + to);
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.from = from;
        this.to = to;
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
    }

    public String getStreamId() {
    	return streamId;
    }

    public int getStreamPartition() {
    	return streamPartition;
    }

    public MessageRef getFrom() {
    	return from;
    }

    public MessageRef getTo() {
    	return to;
    }

    public Address getPublisherId() {
    	return publisherId;
    }

    public String getMsgChainId() {
    	return msgChainId;
    }
}
