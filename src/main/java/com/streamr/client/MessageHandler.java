package com.streamr.client;

import com.streamr.client.protocol.message_layer.StreamMessage;

public interface MessageHandler {
    void onMessage(Subscription sub, StreamMessage message);
    default void done(Subscription sub) {}
}
