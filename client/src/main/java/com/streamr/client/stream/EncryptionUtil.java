package com.streamr.client.stream;

import com.streamr.client.crypto.KeysRsa;
import com.streamr.client.protocol.message_layer.EncryptedGroupKey;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.UnableToDecryptException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
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

  static byte[] decryptWithPrivateKey(final RSAPrivateKey privateKey, String ciphertextHex)
      throws UnableToDecryptException {
    byte[] encryptedBytes = Numeric.hexStringToByteArray(ciphertextHex);
    try {
      rsaCipher.get().init(Cipher.DECRYPT_MODE, privateKey);
      return rsaCipher.get().doFinal(encryptedBytes);
    } catch (Exception e) {
      throw UnableToDecryptException.create(ciphertextHex);
    }
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
    byte[] iv = Numeric.hexStringToByteArray(ciphertext.substring(0, 32));
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    aesCipher.get().init(Cipher.DECRYPT_MODE, groupKey.toSecretKey(), ivParameterSpec);
    return aesCipher.get().doFinal(Numeric.hexStringToByteArray(ciphertext.substring(32)));
  }

  public static EncryptedGroupKey encryptGroupKey(
      GroupKey keyToEncrypt, GroupKey keyToEncryptWith) {
    byte[] plaintext = Numeric.hexStringToByteArray(keyToEncrypt.getGroupKeyHex());
    String encrypted = encrypt(plaintext, keyToEncryptWith);
    return new EncryptedGroupKey(keyToEncrypt.getGroupKeyId(), encrypted, null);
  }

  public static GroupKey decryptGroupKey(EncryptedGroupKey keyToDecrypt, GroupKey keyToDecryptWith)
      throws Exception {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decrypt(keyToDecrypt.getEncryptedGroupKeyHex(), keyToDecryptWith)));
  }

  static EncryptedGroupKey encryptWithPublicKey(
      GroupKey keyToEncrypt, RSAPublicKey keyToEncryptWith) {
    String encryptedGroupKeyHex =
        encryptWithPublicKey(keyToEncrypt.getGroupKeyHex(), keyToEncryptWith);
    return new EncryptedGroupKey(keyToEncrypt.getGroupKeyId(), encryptedGroupKeyHex, null);
  }

  static String encryptWithPublicKey(String plaintextHex, RSAPublicKey publicKey) {
    byte[] plaintext = Numeric.hexStringToByteArray(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
  }

  static String encryptWithPublicKey(byte[] plaintext, String publicKey) {
    KeysRsa.validatePublicKey(publicKey);
    return encryptWithPublicKey(plaintext, KeysRsa.getPublicKeyFromString(publicKey));
  }

  private static String encryptWithPublicKey(byte[] plaintext, RSAPublicKey rsaPublicKey) {
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

  static GroupKey decryptWithPrivateKey(
      final RSAPrivateKey rsaPrivateKey, EncryptedGroupKey keyToDecrypt)
      throws UnableToDecryptException, InvalidGroupKeyException {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(
            decryptWithPrivateKey(rsaPrivateKey, keyToDecrypt.getEncryptedGroupKeyHex())));
  }
}
