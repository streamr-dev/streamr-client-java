package com.streamr.client.options;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import com.streamr.client.protocol.utils.Address;
import java.util.Date;

public class ResendRangeOption extends ResendOption {
  private MessageRef from;
  private MessageRef to;
  private Address publisherId;
  private String msgChainId;

  public ResendRangeOption(
      Date fromTimestamp,
      long fromSequenceNumber,
      Date toTimestamp,
      long toSequenceNumber,
      Address publisherId,
      String msgChainId) {
    from = new MessageRef(fromTimestamp.getTime(), fromSequenceNumber);
    to = new MessageRef(toTimestamp.getTime(), toSequenceNumber);
    this.publisherId = publisherId;
    this.msgChainId = msgChainId;
  }

  public ResendRangeOption(Date fromTimestamp, Date toTimestamp) {
    from = new MessageRef(fromTimestamp.getTime(), 0L);
    to = new MessageRef(toTimestamp.getTime(), 0L);
    this.publisherId = null;
    this.msgChainId = null;
  }

  @Override
  public ControlMessage toRequest(
      String requestId, String streamId, int streamPartition, String sessionToken) {
    return new ResendRangeRequest(
        requestId, streamId, streamPartition, from, to, publisherId, msgChainId, sessionToken);
  }

  public MessageRef getTo() {
    return to;
  }
}
