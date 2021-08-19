package com.streamr.client.subs;

public class AlreadySubscribedException extends RuntimeException {
  public AlreadySubscribedException(Subscription sub) {
    super(
        "Already subscribed to streamId: "
            + sub.getStreamId()
            + ", partition: "
            + sub.getPartition());
  }
}
