package com.streamr.client.utils;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.utils.Address;

public class GapFillFailedException extends RuntimeException {
  public GapFillFailedException(
      MessageRef from, MessageRef to, Address publisherId, String msgChainId, int maxRequests) {
    super(
        "Failed to fill gap between "
            + from
            + " and "
            + to
            + " for "
            + publisherId
            + "-"
            + msgChainId
            + " after "
            + maxRequests
            + " trials");
  }
}
