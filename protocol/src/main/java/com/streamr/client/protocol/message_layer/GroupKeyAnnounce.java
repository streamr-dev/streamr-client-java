package com.streamr.client.protocol.message_layer;

import com.streamr.client.protocol.java.util.Objects;
import com.streamr.client.protocol.utils.EncryptedGroupKey;
import java.util.List;

public final class GroupKeyAnnounce extends AbstractGroupKeyMessage {
  private final List<EncryptedGroupKey> groupKeys;

  public GroupKeyAnnounce(String streamId, List<EncryptedGroupKey> groupKeys) {
    super(streamId);
    Objects.requireNonNull(groupKeys, "groupKeys");
    this.groupKeys = groupKeys;
  }

  public List<EncryptedGroupKey> getKeys() {
    return groupKeys;
  }

  @Override
  protected StreamMessage.MessageType getMessageType() {
    return StreamMessage.MessageType.GROUP_KEY_ANNOUNCE;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final GroupKeyAnnounce that = (GroupKeyAnnounce) obj;
    return Objects.equals(groupKeys, that.groupKeys) && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupKeys, streamId);
  }

  @Override
  public String toString() {
    return String.format("GroupKeyAnnounce{streamId=%s, groupKeys=%s}", streamId, groupKeys);
  }
}
