package com.streamr.client;

import com.streamr.client.protocol.message_layer.StreamMessage;
import java.util.concurrent.*;

public interface MessageHandler {
    void onMessage(Subscription sub, StreamMessage message);
    default void done(Subscription sub) {}
}
