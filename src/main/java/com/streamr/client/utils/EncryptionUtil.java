package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.exceptions.InvalidRSAKeyException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class EncryptionUtil {
    private static final Logger log = LogManager.getLogger();
    private static final SecureRandom SRAND = new SecureRandom();
    private static final ThreadLocal<Cipher> aesCipher = ThreadLocal.withInitial(() -> getAESCipher());
    private static final ThreadLocal<Cipher> rsaCipher = ThreadLocal.withInitial(() -> getRSACipher());

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public EncryptionUtil(String publicKey, String privateKey) {
        if (publicKey == null && privateKey == null) {
            KeyPair pair = generateKeyPair();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
        } else {
            validatePublicKey(publicKey);
            validatePrivateKey(privateKey);
            this.publicKey = getPublicKeyFromString(publicKey);
            this.privateKey = getPrivateKeyFromString(privateKey);
        }
    }

    public EncryptionUtil() {
        this(null, null);
    }

    public byte[] decryptWithPrivateKey(String ciphertext) throws UnableToDecryptException {
        byte[] encryptedBytes = DatatypeConverter.parseHexBinary(ciphertext);
        try {
            rsaCipher.get().init(Cipher.DECRYPT_MODE, this.privateKey);
            return rsaCipher.get().doFinal(encryptedBytes);
        } catch (Exception e) {
            throw new UnableToDecryptException(ciphertext);
        }
    }

    public String getPublicKeyAsPemString() {
        return exportKeyAsPemString(this.publicKey, true);
    }

    public static String encryptWithPublicKey(byte[] plaintext, String publicKey) {
        validatePublicKey(publicKey);
        RSAPublicKey rsaPublicKey = getPublicKeyFromString(publicKey);
        try {
            rsaCipher.get().init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            return Hex.encodeHexString(rsaCipher.get().doFinal(plaintext));
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    public static String encryptWithPublicKey(String plaintextHex, String publicKey) {
        byte[] plaintext = DatatypeConverter.parseHexBinary(plaintextHex);
        return encryptWithPublicKey(plaintext, publicKey);
    }

    public static String encrypt(byte[] plaintext, SecretKey groupKey) {
        try {
            byte[] iv = new byte[16];
            SRAND.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            aesCipher.get().init(Cipher.ENCRYPT_MODE, groupKey, ivspec);
            byte[] ciphertext = aesCipher.get().doFinal(plaintext);
            return Hex.encodeHexString(iv) + Hex.encodeHexString(ciphertext);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public static byte[] decrypt(String ciphertext, SecretKey groupKey) throws Exception {
        if (groupKey == null) throw new InvalidGroupKeyException(0);
        byte[] iv = DatatypeConverter.parseHexBinary(ciphertext.substring(0, 32));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        aesCipher.get().init(Cipher.DECRYPT_MODE, groupKey, ivParameterSpec);
        return aesCipher.get().doFinal(DatatypeConverter.parseHexBinary(ciphertext.substring(32)));
    }

    /*
    Sets the content of 'streamMessage' with the encryption result of the old content with 'groupKey'.
     */
    public static void encryptStreamMessage(StreamMessage streamMessage, SecretKey groupKey) {
        streamMessage.setEncryptionType(StreamMessage.EncryptionType.AES);
        try {
            streamMessage.setSerializedContent(encrypt(streamMessage.getSerializedContentAsBytes(), groupKey));
        } catch (IOException e) {
            log.error(e);
        }
    }

    /*
    Sets the content of 'streamMessage' with the encryption result of a plaintext with 'groupKey'. The
    plaintext is the concatenation of 'newGroupKeyHex' and the old serialized content of 'streamMessage'.
     */
    public static void encryptStreamMessageAndNewKey(String newGroupKeyHex, StreamMessage streamMessage, SecretKey groupKey) {
        streamMessage.setEncryptionType(StreamMessage.EncryptionType.NEW_KEY_AND_AES);
        byte[] groupKeyBytes = DatatypeConverter.parseHexBinary(newGroupKeyHex);
        byte[] payloadBytes = streamMessage.getSerializedContentAsBytes();
        byte[] plaintext = new byte[groupKeyBytes.length + payloadBytes.length];
        System.arraycopy(groupKeyBytes, 0, plaintext, 0, groupKeyBytes.length);
        System.arraycopy(payloadBytes, 0, plaintext, groupKeyBytes.length, payloadBytes.length);
        try {
            streamMessage.setSerializedContent(encrypt(plaintext, groupKey));
        } catch (IOException e) {
            log.error(e);
        }
    }

    /*
    Decrypts the serialized content of 'streamMessage' with 'groupKey'. If the resulting plaintext is the concatenation
    of a new group key and a message content, sets the content of 'streamMessage' with that message content and returns
    the key. If the resulting plaintext is only a message content, sets the content of 'streamMessage' with that
    message content and returns null.
     */
    public static SecretKey decryptStreamMessage(StreamMessage streamMessage, SecretKey groupKey) throws UnableToDecryptException {
        StreamMessage.EncryptionType encryptionType = streamMessage.getEncryptionType();
        try {
            if (encryptionType == StreamMessage.EncryptionType.AES) {
                streamMessage.setEncryptionType(StreamMessage.EncryptionType.NONE);
                streamMessage.setSerializedContent(decrypt(streamMessage.getSerializedContent(), groupKey));
            } else if (encryptionType == StreamMessage.EncryptionType.NEW_KEY_AND_AES) {
                byte[] plaintext = EncryptionUtil.decrypt(streamMessage.getSerializedContent(), groupKey);
                streamMessage.setEncryptionType(StreamMessage.EncryptionType.NONE);
                streamMessage.setSerializedContent(Arrays.copyOfRange(plaintext, 32, plaintext.length));
                return new SecretKeySpec(Arrays.copyOfRange(plaintext, 0, 32), "AES");
            }
        } catch (Exception e) {
            streamMessage.setEncryptionType(encryptionType);
            throw new UnableToDecryptException(streamMessage.getSerializedContent());
        }
        return null;
    }

    public static void validateGroupKey(String groupKeyHex) {
        String without0x = groupKeyHex.startsWith("0x") ? groupKeyHex.substring(2) : groupKeyHex;
        if (without0x.length() != 64) { // the key must be 256 bits long
            throw new InvalidGroupKeyException(without0x.length() * 4);
        }
    }

    public static void validatePublicKey(String publicKey) {
        if (publicKey == null || !publicKey.startsWith("-----BEGIN RSA PUBLIC KEY-----")
                || !publicKey.endsWith("-----END RSA PUBLIC KEY-----\n")) {
            throw new InvalidRSAKeyException(true);
        }
    }

    public static void validatePrivateKey(String privateKey) {
        if (privateKey == null || !privateKey.startsWith("-----BEGIN RSA PRIVATE KEY-----")
                || !privateKey.endsWith("-----END RSA PRIVATE KEY-----\n")) {
            throw new InvalidRSAKeyException(false);
        }
    }

    public static String exportKeyAsPemString(Key key, boolean isPublic) {
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        try {
            pemWriter.writeObject(new PemObject("RSA " + (isPublic ? "PUBLIC" : "PRIVATE")  + " KEY", key.getEncoded()));
            pemWriter.flush();
            pemWriter.close();
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private static Cipher getAESCipher() {
        try {
            return Cipher.getInstance("AES/CTR/NoPadding");
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static Cipher getRSACipher() {
        try {
            return Cipher.getInstance("RSA");
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static RSAPublicKey getPublicKeyFromString(String publicKey) {
        publicKey = publicKey.replace("-----BEGIN RSA PUBLIC KEY-----\n", "");
        publicKey = publicKey.replace("-----END RSA PUBLIC KEY-----", "");
        byte[] encoded = Base64.decodeBase64(publicKey);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static RSAPrivateKey getPrivateKeyFromString(String privateKey) {
        privateKey = privateKey.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
        privateKey = privateKey.replace("-----END RSA PRIVATE KEY-----", "");
        byte[] encoded = Base64.decodeBase64(privateKey);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096, new SecureRandom());
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
}
