package com.streamr.client.stream;

import com.streamr.client.crypto.KeysRsa;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.UnableToDecryptException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
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

  private static Cipher getAESCipher() {
    final String transformation = "AES/CTR/NoPadding";
    try {
      return Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      String format = String.format("%s is not avaible.", transformation);
      throw new IllegalArgumentException(format, e);
    }
  }

  private static Cipher getRSACipher() {
    final String transformation = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    try {
      return Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      String format = String.format("%s is not avaible.", transformation);
      throw new IllegalArgumentException(format, e);
    }
  }

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

  public static String encrypt(byte[] plaintext, GroupKey groupKey) {
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

  public static byte[] decrypt(String ciphertext, GroupKey groupKey) throws Exception {
    if (groupKey == null) throw new InvalidGroupKeyException(0);
    byte[] iv = DatatypeConverter.parseHexBinary(ciphertext.substring(0, 32));
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    aesCipher.get().init(Cipher.DECRYPT_MODE, groupKey.toSecretKey(), ivParameterSpec);
    return aesCipher.get().doFinal(DatatypeConverter.parseHexBinary(ciphertext.substring(32)));
  }

  public static EncryptedGroupKey encryptGroupKey(
      GroupKey keyToEncrypt, GroupKey keyToEncryptWith) {
    return new EncryptedGroupKey(
        keyToEncrypt.getGroupKeyId(),
        encrypt(DatatypeConverter.parseHexBinary(keyToEncrypt.getGroupKeyHex()), keyToEncryptWith));
  }

  public static GroupKey decryptGroupKey(EncryptedGroupKey keyToDecrypt, GroupKey keyToDecryptWith)
      throws Exception {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decrypt(keyToDecrypt.getEncryptedGroupKeyHex(), keyToDecryptWith)));
  }

  public static EncryptedGroupKey encryptWithPublicKey(
      GroupKey keyToEncrypt, RSAPublicKey keyToEncryptWith) {
    return new EncryptedGroupKey(
        keyToEncrypt.getGroupKeyId(),
        encryptWithPublicKey(keyToEncrypt.getGroupKeyHex(), keyToEncryptWith));
  }

  static String encryptWithPublicKey(String plaintextHex, String publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
  }

  static String encryptWithPublicKey(String plaintextHex, RSAPublicKey publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
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

  /**
   * Sets the content of 'streamMessage' with the encryption result of the old content with
   * 'groupKey'.
   */
  public static StreamMessage encryptStreamMessage(
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

  public static GroupKey decryptWithPrivateKey(
      final RSAPrivateKey rsaPrivateKey, EncryptedGroupKey keyToDecrypt)
      throws UnableToDecryptException, InvalidGroupKeyException {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decryptWithPrivateKey(rsaPrivateKey, keyToDecrypt.getEncryptedGroupKeyHex())));
  }
}