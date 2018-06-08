package com.streamr.client;

import com.streamr.client.protocol.StreamMessage;

public interface MessageHandler {
    void onMessage(Subscription sub, StreamMessage message);
}
