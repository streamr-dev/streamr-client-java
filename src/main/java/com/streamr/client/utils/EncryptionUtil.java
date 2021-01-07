package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.exceptions.InvalidRSAKeyException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Key;
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
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
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

  private final RSAPublicKey publicKey;
  private final RSAPrivateKey privateKey;

  public EncryptionUtil(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
    if (rsaPublicKey == null && rsaPrivateKey == null) {
      KeyPair pair = generateKeyPair();
      this.publicKey = (RSAPublicKey) pair.getPublic();
      this.privateKey = (RSAPrivateKey) pair.getPrivate();
    } else {
      this.publicKey = rsaPublicKey;
      this.privateKey = rsaPrivateKey;
    }
  }

  public EncryptionUtil() {
    this(null, null);
  }

  public byte[] decryptWithPrivateKey(String ciphertextHex) throws UnableToDecryptException {
    byte[] encryptedBytes = DatatypeConverter.parseHexBinary(ciphertextHex);
    try {
      rsaCipher.get().init(Cipher.DECRYPT_MODE, this.privateKey);
      return rsaCipher.get().doFinal(encryptedBytes);
    } catch (Exception e) {
      throw new UnableToDecryptException(ciphertextHex);
    }
  }

  StreamMessage decryptWithPrivateKey(final StreamMessage msg) throws UnableToDecryptException {
    if (msg.getEncryptionType() != StreamMessage.EncryptionType.RSA) {
      throw new IllegalArgumentException("Given StreamMessage is not encrypted with RSA!");
    }
    return new StreamMessage.Builder(msg)
        .setEncryptionType(StreamMessage.EncryptionType.NONE)
        .setSerializedContent(decryptWithPrivateKey(msg.getSerializedContent()))
        .createStreamMessage();
  }

  public RSAPublicKey getPublicKey() {
    return this.publicKey;
  }

  public String getPublicKeyAsPemString() {
    return exportKeyAsPemString(this.publicKey, true);
  }

  public static void encryptWithPublicKey(StreamMessage msg, String publicKey) {
    msg.setEncryptionType(StreamMessage.EncryptionType.RSA);
    msg.setGroupKeyId(publicKey);
    msg.setSerializedContent(encryptWithPublicKey(msg.getSerializedContentAsBytes(), publicKey));
  }

  public static String encryptWithPublicKey(byte[] plaintext, String publicKey) {
    validatePublicKey(publicKey);
    return encryptWithPublicKey(plaintext, getPublicKeyFromString(publicKey));
  }

  public static String encryptWithPublicKey(byte[] plaintext, RSAPublicKey rsaPublicKey) {
    try {
      rsaCipher.get().init(Cipher.ENCRYPT_MODE, rsaPublicKey);
      return Numeric.toHexStringNoPrefix(rsaCipher.get().doFinal(plaintext));
    } catch (Exception e) {
      log.error("Failed to encrypt plaintext: {}", Arrays.toString(plaintext), e);
      throw new RuntimeException(e);
    }
  }

  public static String encryptWithPublicKey(String plaintextHex, String publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
  }

  public static String encryptWithPublicKey(String plaintextHex, RSAPublicKey publicKey) {
    byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
    return encryptWithPublicKey(plaintext, publicKey);
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

  public GroupKey decryptWithPrivateKey(EncryptedGroupKey keyToDecrypt)
      throws UnableToDecryptException, InvalidGroupKeyException {
    return new GroupKey(
        keyToDecrypt.getGroupKeyId(),
        Numeric.toHexStringNoPrefix(decryptWithPrivateKey(keyToDecrypt.getEncryptedGroupKeyHex())));
  }

  /**
   * Sets the content of 'streamMessage' with the encryption result of the old content with
   * 'groupKey'.
   */
  public static void encryptStreamMessage(StreamMessage streamMessage, GroupKey groupKey)
      throws InvalidGroupKeyException {
    streamMessage.setSerializedContent(
        encrypt(streamMessage.getSerializedContentAsBytes(), groupKey));
    streamMessage.setEncryptionType(StreamMessage.EncryptionType.AES);
    streamMessage.setGroupKeyId(groupKey.getGroupKeyId());
  }

  /** Decrypts the serialized content of 'streamMessage' with 'groupKey'. */
  public static StreamMessage decryptStreamMessage(final StreamMessage streamMessage, final GroupKey groupKey)
      throws UnableToDecryptException {
    if (streamMessage.getEncryptionType() != StreamMessage.EncryptionType.AES) {
      throw new IllegalArgumentException("Given StreamMessage is not encrypted with AES!");
    }

    try {
      final byte[] decryptedContent = decrypt(streamMessage.getSerializedContent(), groupKey);
      return new StreamMessage.Builder(streamMessage)
          .setSerializedContent(decryptedContent)
          .setEncryptionType(StreamMessage.EncryptionType.NONE)
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

      throw new UnableToDecryptException(streamMessage.getSerializedContent());
    }
  }

  public static void validateGroupKey(String groupKeyHex) throws InvalidGroupKeyException {
    String without0x = groupKeyHex.startsWith("0x") ? groupKeyHex.substring(2) : groupKeyHex;
    if (without0x.length() != 64) { // the key must be 256 bits long
      throw new InvalidGroupKeyException(without0x.length() * 4);
    }
  }

  public static void validatePublicKey(String publicKey) {
    if (publicKey == null
        || !publicKey.startsWith("-----BEGIN PUBLIC KEY-----")
        || !publicKey.endsWith("-----END PUBLIC KEY-----\n")) {
      throw new InvalidRSAKeyException(true);
    }
  }

  public static void validatePrivateKey(String privateKey) {
    if (privateKey == null
        || !privateKey.startsWith("-----BEGIN PRIVATE KEY-----")
        || !privateKey.endsWith("-----END PRIVATE KEY-----\n")) {
      throw new InvalidRSAKeyException(false);
    }
  }

  public static String exportKeyAsPemString(Key key, boolean isPublic) {
    StringWriter writer = new StringWriter();
    PemWriter pemWriter = new PemWriter(writer);
    try {
      pemWriter.writeObject(
          new PemObject((isPublic ? "PUBLIC" : "PRIVATE") + " KEY", key.getEncoded()));
      pemWriter.flush();
    } catch (IOException e) {
      log.error("Failed to write key as PEM", e);
      throw new RuntimeException(e);
    } finally {
      try {
        pemWriter.close();
      } catch (IOException e) {
        log.error("Failed to close PemWriter", e);
      }
    }
    return writer.toString();
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

  public static SecretKey getSecretKeyFromHexString(String groupKeyHex)
      throws InvalidGroupKeyException {
    EncryptionUtil.validateGroupKey(groupKeyHex);
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
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }
    return new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
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
