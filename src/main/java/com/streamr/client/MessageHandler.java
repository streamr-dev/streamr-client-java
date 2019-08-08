package com.streamr.client;

import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.subs.Subscription;

public interface MessageHandler {
    void onMessage(Subscription sub, StreamMessage message);
    default void done(Subscription sub) {}
}
