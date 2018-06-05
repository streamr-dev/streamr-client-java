package com.streamr.client.exceptions;

import com.streamr.client.Subscription;

public class AlreadySubscribedException extends RuntimeException {
    public AlreadySubscribedException(Subscription sub) {
        super("Already subscribed to streamId: " + sub.getStreamId() + ", partition: " + sub.getPartition());
    }
}
