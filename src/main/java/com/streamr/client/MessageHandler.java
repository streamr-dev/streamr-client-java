package com.streamr.client;

import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.subs.Subscription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface MessageHandler {
    Logger log = LogManager.getLogger();
    void onMessage(Subscription sub, StreamMessage message);
    default void done(Subscription sub) {}
    default void onUnableToDecrypt(UnableToDecryptException e) { log.warn(e); }
}
