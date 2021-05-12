package com.streamr.client;

import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.subs.Subscription;
import com.streamr.client.utils.UnableToDecryptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MessageHandler {
  Logger log = LoggerFactory.getLogger(MessageHandler.class);

  void onMessage(Subscription sub, StreamMessage message);

  default void done(Subscription sub) {}

  default void onUnableToDecrypt(UnableToDecryptException e) {
    log.warn("Unable to decrypt", e);
  }
}
