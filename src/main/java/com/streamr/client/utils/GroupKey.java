package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import org.web3j.utils.Numeric;

public class GroupKey {

  private static final SecureRandom defaultSecureRandom = new SecureRandom();

  protected final String groupKeyId;
  protected final String groupKeyHex;
  private SecretKey cachedSecretKey;

  public GroupKey(String groupKeyId, String groupKeyHex) throws InvalidGroupKeyException {
    this.groupKeyId = groupKeyId;
    this.groupKeyHex = groupKeyHex;
    cachedSecretKey = EncryptionUtil.getSecretKeyFromHexString(groupKeyHex);
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
  public boolean equals(Object other) {
    if (!(other instanceof GroupKey)) {
      return false;
    }
    GroupKey o = (GroupKey) other;
    return groupKeyHex.equals(o.groupKeyHex) && groupKeyId.equals(o.groupKeyId);
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
