package com.streamr.client.exceptions;

import com.streamr.client.protocol.message_layer.MessageRef;

public class GapFillFailedException extends RuntimeException {
    public GapFillFailedException(MessageRef from, MessageRef to, String publisherId, String msgChainId, int maxRequests) {
        super("Failed to fill gap between " + from + " and " + to + " for " + publisherId + "-"
                + msgChainId + " after " + maxRequests + " trials");
    }
}
