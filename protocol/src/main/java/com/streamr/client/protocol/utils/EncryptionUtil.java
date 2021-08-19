package com.streamr.client.protocol.utils;

import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
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

  private final RSAKeyPair keyPair;

  public EncryptionUtil(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
    if (rsaPublicKey == null && rsaPrivateKey == null) {
      this.keyPair = RSAKeyPair.create();
    } else {
      this.keyPair = new RSAKeyPair(rsaPublicKey, rsaPrivateKey);
    }
  }

  public EncryptionUtil() {
    this(null, null);
  }

  public byte[] decryptWithPrivateKey(String ciphertextHex) throws UnableToDecryptException {
    byte[] encryptedBytes = DatatypeConverter.parseHexBinary(ciphertextHex);
    try {
      rsaCipher.get().init(Cipher.DECRYPT_MODE, this.keyPair.getPrivateKey());
      return rsaCipher.get().doFinal(encryptedBytes);
    } catch (Exception e) {
      throw UnableToDecryptException.create(ciphertextHex);
    }
  }

  StreamMessage decryptWithPrivateKey(final StreamMessage msg) throws UnableToDecryptException {
    if (msg.getEncryptionType() != StreamMessage.EncryptionType.RSA) {
      throw new IllegalArgumentException("Given StreamMessage is not encrypted with RSA!");
    }
    final byte[] payload = decryptWithPrivateKey(msg.getSerializedContent());
    final StreamMessage.Content content = StreamMessage.Content.Factory.withJsonAsPayload(payload);
    return new StreamMessage.Builder(msg)
        .withEncryptionType(StreamMessage.EncryptionType.NONE)
        .withContent(content)
        .createStreamMessage();
  }

  RSAPublicKey getPublicKey() {
    return this.keyPair.getPublicKey();
  }

  public String getPublicKeyAsPemString() {
    return this.keyPair.publicKeyToPem();
  }

  static StreamMessage encryptWithPublicKey(final StreamMessage msg, final String publicKey) {
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
    validatePublicKey(publicKey);
    return encryptWithPublicKey(plaintext, getPublicKeyFromString(publicKey));
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

  static EncryptedGroupKey encryptWithPublicKey(
      GroupKey keyToEncrypt, RSAPublicKey keyToEncryptWith) {
    return new EncryptedGroupKey(
        keyToEncrypt.getGroupKeyId(),
        encryptWithPublicKey(keyToEncrypt.getGroupKeyHex(), keyToEncryptWith));
  }

  GroupKey decryptWithPrivateKey(EncryptedGroupKey keyToDecrypt)
      throws UnableToDecryptException, InvalidGroupKeyException {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(decryptWithPrivateKey(keyToDecrypt.getEncryptedGroupKeyHex())));
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

  public static void validatePublicKey(String publicKey) {
    if (publicKey == null
        || !publicKey.startsWith("-----BEGIN PUBLIC KEY-----")
        || !publicKey.endsWith("-----END PUBLIC KEY-----\n")) {
      throw new RuntimeException("Must be a valid RSA public key in the PEM format.");
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

  public static RSAPublicKey getPublicKeyFromString(String publicKey) {
    publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----\n", "");
    publicKey = publicKey.replace("-----END PUBLIC KEY-----", "");
    byte[] encoded = Base64.getMimeDecoder().decode(publicKey);
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
    } catch (Exception e) {
      log.error("Failed to parse public key from string: " + publicKey, e);
      throw new RuntimeException(e);
    }
  }

  public static RSAPrivateKey getPrivateKeyFromString(String privateKey) {
    privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----\n", "");
    privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
    byte[] encoded = Base64.getMimeDecoder().decode(privateKey);
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
      return (RSAPrivateKey) kf.generatePrivate(keySpec);
    } catch (Exception e) {
      log.error("Failed to parse private key", e);
      throw new RuntimeException(e);
    }
  }

  public static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(4096, SRAND);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
