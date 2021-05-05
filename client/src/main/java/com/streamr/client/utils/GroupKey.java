package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.java.util.Objects;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import org.web3j.utils.Numeric;

public final class GroupKey {
  private static final SecureRandom defaultSecureRandom = new SecureRandom();
  private final String groupKeyId;
  private final String groupKeyHex;
  private final SecretKey cachedSecretKey;

  public GroupKey(String groupKeyId, String groupKeyHex) throws InvalidGroupKeyException {
    this.groupKeyId = groupKeyId;
    this.groupKeyHex = groupKeyHex;
    this.cachedSecretKey = EncryptionUtil.getSecretKeyFromHexString(groupKeyHex);
  }

  public String getGroupKeyId() {
    return groupKeyId;
  }

  public String getGroupKeyHex() {
    return groupKeyHex;
  }

  public SecretKey toSecretKey() {
    return cachedSecretKey;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    GroupKey o = (GroupKey) obj;
    return Objects.equals(groupKeyId, o.groupKeyId)
        && Objects.equals(groupKeyHex, o.groupKeyHex)
        && Objects.equals(cachedSecretKey, o.cachedSecretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupKeyId, groupKeyHex, cachedSecretKey);
  }

  @Override
  public String toString() {
    return String.format("GroupKey{groupKeyId=%s}", groupKeyId);
  }

  public static GroupKey generate() {
    return GroupKey.generate(IdGenerator.get(), defaultSecureRandom);
  }

  public static GroupKey generate(String id) {
    return GroupKey.generate(id, defaultSecureRandom);
  }

  public static GroupKey generate(String id, SecureRandom secureRandom) {
    byte[] keyBytes = new byte[32];
    secureRandom.nextBytes(keyBytes);
    try {
      return new GroupKey(id, Numeric.toHexStringNoPrefix(keyBytes));
    } catch (InvalidGroupKeyException e) {
      throw new RuntimeException(e);
    }
  }
}
