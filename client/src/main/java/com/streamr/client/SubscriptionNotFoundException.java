package com.streamr.client;

public class SubscriptionNotFoundException extends Exception {
  public SubscriptionNotFoundException(String streamId, int partition) {
    super("Subscription not found! streamId: " + streamId + ", partition: " + partition);
  }
}
