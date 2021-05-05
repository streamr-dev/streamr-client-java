package com.streamr.client.utils;

import com.streamr.client.java.util.Objects;
import com.streamr.client.protocol.message_layer.EncryptedGroupKeyAdapter;
import java.io.IOException;

/**
 * A container for encrypted group keys. Used to get compile-time checking that encrypted and
 * plaintext keys don't accidentally mix.
 */
public final class EncryptedGroupKey {
  private final String groupKeyId;
  private final String encryptedGroupKeyHex;
  private String serialized;

  public EncryptedGroupKey(String groupKeyId, String encryptedGroupKeyHex) {
    this(groupKeyId, encryptedGroupKeyHex, null);
  }

  /**
   * @param serialized used to cache the exact serialized form of the EncryptedGroupKey, useful for
   *     validation
   */
  public EncryptedGroupKey(String groupKeyId, String encryptedGroupKeyHex, String serialized) {
    this.groupKeyId = groupKeyId;
    this.encryptedGroupKeyHex = encryptedGroupKeyHex;
    this.serialized = serialized;
  }

  public String getGroupKeyId() {
    return groupKeyId;
  }

  public String getEncryptedGroupKeyHex() {
    return encryptedGroupKeyHex;
  }

  public String getSerialized() {
    return serialized;
  }

  public void setSerialized(String serialized) {
    this.serialized = serialized;
  }

  public String serialize() {
    if (serialized != null) {
      return serialized;
    } else {
      return new EncryptedGroupKeyAdapter().toJson(this);
    }
  }

  public static EncryptedGroupKey deserialize(String serialized) throws IOException {
    EncryptedGroupKey result = new EncryptedGroupKeyAdapter().fromJson(serialized);
    result.setSerialized(serialized);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final EncryptedGroupKey that = (EncryptedGroupKey) obj;
    return Objects.equals(groupKeyId, that.groupKeyId)
        && Objects.equals(encryptedGroupKeyHex, that.encryptedGroupKeyHex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupKeyId, encryptedGroupKeyHex);
  }

  @Override
  public String toString() {
    return String.format("EncryptedGroupKey{groupKeyId=%s}", groupKeyId);
  }
}
