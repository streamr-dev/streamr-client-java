package com.streamr.client.utils;

import com.streamr.client.crypto.KeysRsa;
import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.utils.Numeric;

public final class EncryptionUtil {
  private static final Logger log = LoggerFactory.getLogger(EncryptionUtil.class);
  private static final SecureRandom SRAND = new SecureRandom();
  private static final ThreadLocal<Cipher> aesCipher =
      ThreadLocal.withInitial(() -> getAESCipher());
  private static final ThreadLocal<Cipher> rsaCipher =
      ThreadLocal.withInitial(() -> getRSACipher());

  private EncryptionUtil() {}

  public static byte[] decryptWithPrivateKey(final RSAPrivateKey privateKey, String ciphertextHex)
      throws UnableToDecryptException {
    byte[] encryptedBytes = DatatypeConverter.parseHexBinary(ciphertextHex);
    try {
      rsaCipher.get().init(Cipher.DECRYPT_MODE, privateKey);
      return rsaCipher.get().doFinal(encryptedBytes);
    } catch (Exception e) {
      throw UnableToDecryptException.create(ciphertextHex);
    }
  }

  static StreamMessage decryptWithPrivateKey(final RSAPrivateKey rsaPrivateKey, final StreamMessage msg) throws UnableToDecryptException {
    if (msg.getEncryptionType() != StreamMessage.EncryptionType.RSA) {
      throw new IllegalArgumentException("Given StreamMessage is not encrypted with RSA!");
    }
    final byte[] payload = decryptWithPrivateKey(rsaPrivateKey, msg.getSerializedContent());
    final StreamMessage.Content content = StreamMessage.Content.Factory.withJsonAsPayload(payload);
    return new StreamMessage.Builder(msg)
        .withEncryptionType(StreamMessage.EncryptionType.NONE)
        .withContent(content)
        .createStreamMessage();
  }

  private static StreamMessage encryptWithPublicKey(
      final StreamMessage msg, final String publicKey) {
    final String message = encryptWithPublicKey(msg.getSerializedContentAsBytes(), publicKey);
    final byte[] payload = message.getBytes(StandardCharsets.UTF_8);
    final StreamMessage.Content content = StreamMessage.Content.Factory.withJsonAsPayload(payload);
    return new StreamMessage.Builder(msg)
        .withEncryptionType(StreamMessage.EncryptionType.RSA)
        .withGroupKeyId(publicKey)
        .withContent(content)
        .createStreamMessage();
  }

  static String encryptWithPublicKey(byte[] plaintext, String publicKey) {
    KeysRsa.validatePublicKey(publicKey);
    return encryptWithPublicKey(plaintext, KeysRsa.getPublicKeyFromString(publicKey));
  }

  static String encryptWithPublicKey(byte[] plaintext, RSAPublicKey rsaPublicKey) {
    try {
      rsaCipher.get().init(Cipher.ENCRYPT_MODE, rsaPublicKey);
      return Numeric.toHexStringNoPrefix(rsaCipher.get().doFinal(plaintext));
    } catch (Exception e) {
      log.error("Failed to encrypt plaintext: {}", Arrays.toString(plaintext), e);
      throw new RuntimeException(e);
    }
  }

  static String encryptWithPublicKey(String plaintextHex, String publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
  }

  static String encryptWithPublicKey(String plaintextHex, RSAPublicKey publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
  }

  static String encrypt(byte[] plaintext, GroupKey groupKey) {
    try {
      byte[] iv = new byte[16];
      SRAND.nextBytes(iv);
      IvParameterSpec ivspec = new IvParameterSpec(iv);
      aesCipher.get().init(Cipher.ENCRYPT_MODE, groupKey.toSecretKey(), ivspec);
      byte[] ciphertext = aesCipher.get().doFinal(plaintext);
      return Numeric.toHexStringNoPrefix(iv) + Numeric.toHexStringNoPrefix(ciphertext);
    } catch (Exception e) {
      log.error("Failed to encrypt with groupKey", e);
    }
    return null;
  }

  static byte[] decrypt(String ciphertext, GroupKey groupKey) throws Exception {
    if (groupKey == null) throw new InvalidGroupKeyException(0);
    byte[] iv = DatatypeConverter.parseHexBinary(ciphertext.substring(0, 32));
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    aesCipher.get().init(Cipher.DECRYPT_MODE, groupKey.toSecretKey(), ivParameterSpec);
    return aesCipher.get().doFinal(DatatypeConverter.parseHexBinary(ciphertext.substring(32)));
  }

  static EncryptedGroupKey encryptGroupKey(GroupKey keyToEncrypt, GroupKey keyToEncryptWith) {
    return new EncryptedGroupKey(
        keyToEncrypt.getGroupKeyId(),
        encrypt(DatatypeConverter.parseHexBinary(keyToEncrypt.getGroupKeyHex()), keyToEncryptWith));
  }

  static GroupKey decryptGroupKey(EncryptedGroupKey keyToDecrypt, GroupKey keyToDecryptWith)
      throws Exception {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decrypt(keyToDecrypt.getEncryptedGroupKeyHex(), keyToDecryptWith)));
  }

  static EncryptedGroupKey encryptWithPublicKey(
      GroupKey keyToEncrypt, RSAPublicKey keyToEncryptWith) {
    return new EncryptedGroupKey(
        keyToEncrypt.getGroupKeyId(),
        encryptWithPublicKey(keyToEncrypt.getGroupKeyHex(), keyToEncryptWith));
  }

  static GroupKey decryptWithPrivateKey(final RSAPrivateKey rsaPrivateKey, EncryptedGroupKey keyToDecrypt)
      throws UnableToDecryptException, InvalidGroupKeyException {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decryptWithPrivateKey(rsaPrivateKey, keyToDecrypt.getEncryptedGroupKeyHex())));
  }

  /**
   * Sets the content of 'streamMessage' with the encryption result of the old content with
   * 'groupKey'.
   */
  static StreamMessage encryptStreamMessage(
      final StreamMessage streamMessage, final GroupKey groupKey) throws InvalidGroupKeyException {
    final String message = encrypt(streamMessage.getSerializedContentAsBytes(), groupKey);
    final StreamMessage.Content content = StreamMessage.Content.Factory.withJsonAsPayload(message);
    return new StreamMessage.Builder(streamMessage)
        .withContent(content)
        .withEncryptionType(StreamMessage.EncryptionType.AES)
        .withGroupKeyId(groupKey.getGroupKeyId())
        .createStreamMessage();
  }

  /** Decrypts the serialized content of 'streamMessage' with 'groupKey'. */
  public static StreamMessage decryptStreamMessage(
      final StreamMessage streamMessage, final GroupKey groupKey) throws UnableToDecryptException {
    if (streamMessage.getEncryptionType() != StreamMessage.EncryptionType.AES) {
      throw new IllegalArgumentException("Given StreamMessage is not encrypted with AES!");
    }

    try {
      final byte[] decryptedContent = decrypt(streamMessage.getSerializedContent(), groupKey);
      final StreamMessage.Content content =
          StreamMessage.Content.Factory.withJsonAsPayload(decryptedContent);
      return new StreamMessage.Builder(streamMessage)
          .withContent(content)
          .withEncryptionType(StreamMessage.EncryptionType.NONE)
          .createStreamMessage();
    } catch (Exception e) {
      if (groupKey == null) {
        log.debug(
            "No key given to decrypt stream {} msg {}",
            streamMessage.getStreamId(),
            streamMessage.getMessageRef());
      } else {
        log.debug(
            "Failed to decrypt stream {} msg {} with key {} ",
            streamMessage.getStreamId(),
            streamMessage.getMessageRef(),
            groupKey.getGroupKeyId());
      }
      throw UnableToDecryptException.create(streamMessage.getSerializedContent());
    }
  }

  private static Cipher getAESCipher() {
    try {
      return Cipher.getInstance("AES/CTR/NoPadding");
    } catch (Exception e) {
      log.error("Failed to get AES cipher", e);
      throw new RuntimeException(e);
    }
  }

  private static Cipher getRSACipher() {
    try {
      return Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
    } catch (Exception e) {
      log.error("Failed to get RSA cipher", e);
      throw new RuntimeException(e);
    }
  }

  public static SecretKey getSecretKeyFromHexString(String groupKeyHex)
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
    return new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
  }
}
