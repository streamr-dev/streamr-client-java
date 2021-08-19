package com.streamr.client.protocol.message_layer;

import com.streamr.client.protocol.java.util.Objects;
import java.util.List;

public final class GroupKeyRequest extends AbstractGroupKeyMessage {
  private final String requestId;
  private final String rsaPublicKey;
  private final List<String> groupKeyIds;

  public GroupKeyRequest(
      String requestId, String streamId, String rsaPublicKey, List<String> groupKeyIds) {
    super(streamId);
    Objects.requireNonNull(requestId, "requestId");
    this.requestId = requestId;
    Objects.requireNonNull(rsaPublicKey, "rsaPublicKey");
    this.rsaPublicKey = rsaPublicKey;
    ValidationUtil.checkNotEmpty(groupKeyIds, "groupKeyIds");
    this.groupKeyIds = groupKeyIds;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getRsaPublicKey() {
    return rsaPublicKey;
  }

  public List<String> getGroupKeyIds() {
    return groupKeyIds;
  }

  @Override
  protected StreamMessage.MessageType getMessageType() {
    return StreamMessage.MessageType.GROUP_KEY_REQUEST;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final GroupKeyRequest that = (GroupKeyRequest) obj;
    return Objects.equals(requestId, that.requestId)
        && Objects.equals(rsaPublicKey, that.rsaPublicKey)
        && Objects.equals(groupKeyIds, that.groupKeyIds)
        && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, rsaPublicKey, groupKeyIds, streamId);
  }

  @Override
  public String toString() {
    return String.format(
        "GroupKeyRequest{requestId=%s, streamId=%s, keys=%s, rsaPublicKey=%s}",
        requestId, streamId, groupKeyIds, rsaPublicKey);
  }
}
