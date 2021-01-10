package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;
import java.util.List;
import java.util.Objects;

public final class GroupKeyRequest extends AbstractGroupKeyMessage {
  private final String requestId;
  private final String publicKey;
  private final List<String> groupKeyIds;

  public GroupKeyRequest(
      String requestId, String streamId, String rsaPublicKey, List<String> groupKeyIds) {
    super(streamId);
    ValidationUtil.checkNotNull(requestId, "requestId");
    ValidationUtil.checkNotNull(rsaPublicKey, "rsaPublicKey");
    ValidationUtil.checkNotNull(groupKeyIds, "groupKeyIds");
    ValidationUtil.checkNotEmpty(groupKeyIds, "groupKeyIds");

    this.requestId = requestId;
    this.publicKey = rsaPublicKey;
    this.groupKeyIds = groupKeyIds;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getPublicKey() {
    return publicKey;
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
        && Objects.equals(publicKey, that.publicKey)
        && Objects.equals(groupKeyIds, that.groupKeyIds)
        && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, publicKey, groupKeyIds, streamId);
  }

  @Override
  public String toString() {
    return String.format(
        "GroupKeyRequest{requestId=%s, streamId=%s, keys=%s, publicKey=%s}",
        requestId, streamId, groupKeyIds, publicKey);
  }
}
