package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.EncryptedGroupKey;
import com.streamr.client.utils.ValidationUtil;
import java.util.List;
import java.util.Objects;

public final class GroupKeyResponse extends AbstractGroupKeyMessage {
  private final String requestId;
  private final List<EncryptedGroupKey> keys;

  public GroupKeyResponse(String requestId, String streamId, List<EncryptedGroupKey> keys) {
    super(streamId);

    ValidationUtil.checkNotNull(requestId, "requestId");
    ValidationUtil.checkNotNull(keys, "keys");
    ValidationUtil.checkNotEmpty(keys, "keys");

    this.requestId = requestId;
    this.keys = keys;
  }

  public String getRequestId() {
    return requestId;
  }

  public List<EncryptedGroupKey> getKeys() {
    return keys;
  }

  @Override
  protected StreamMessage.MessageType getMessageType() {
    return StreamMessage.MessageType.GROUP_KEY_RESPONSE;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final GroupKeyResponse that = (GroupKeyResponse) o;
    return Objects.equals(requestId, that.requestId)
        && Objects.equals(keys, that.keys)
        && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, keys, streamId);
  }

  @Override
  public String toString() {
    return String.format(
        "GroupKeyResponse{requestId=%s, streamId=%s, keys=%s}", requestId, streamId, keys);
  }
}
