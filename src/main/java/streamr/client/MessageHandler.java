package streamr.client;

import streamr.client.protocol.StreamMessage;

public interface MessageHandler {
    void onMessage(Subscription sub, StreamMessage message);
}
