package com.streamr.client.stream;

import com.streamr.client.java.util.Objects;
import com.streamr.client.utils.IdGenerator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.web3j.utils.Numeric;

public final class GroupKey {
  private static final SecureRandom defaultSecureRandom = new SecureRandom();
  private final String groupKeyId;
  private final String groupKeyHex;
  private final SecretKey cachedSecretKey;

  public GroupKey(String groupKeyId, String groupKeyHex) throws InvalidGroupKeyException {
    this.groupKeyId = groupKeyId;
    this.groupKeyHex = groupKeyHex;
    this.cachedSecretKey = getSecretKeyFromHexString(groupKeyHex);
  }

  static void validate(final String groupKeyHex) throws InvalidGroupKeyException {
    final String withoutPrefix = Numeric.cleanHexPrefix(groupKeyHex);
    if (withoutPrefix.length() != 64) { // the key must be 256 bits long
      throw new InvalidGroupKeyException(withoutPrefix.length() * 4);
    }
  }

  private static SecretKey getSecretKeyFromHexString(String groupKeyHex)
      throws InvalidGroupKeyException {
    GroupKey.validate(groupKeyHex);
    try {
      // need to modify "isRestricted" field to be able to use keys longer than 128 bits.
      Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
      field.setAccessible(true);

      // "isRestricted" is final so we must remove the 'final' modifier in order to change the field
      // value
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      // removes the restriction for key size by setting "isRestricted" to false
      field.set(null, false);
    } catch (ClassNotFoundException
        | NoSuchFieldException
        | SecurityException
        | IllegalArgumentException
        | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
    return new SecretKeySpec(Numeric.hexStringToByteArray(groupKeyHex), "AES");
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
