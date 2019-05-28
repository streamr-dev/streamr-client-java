package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

public class EncryptionUtil {
    private static final Logger log = LogManager.getLogger();
    private static final SecureRandom SRAND = new SecureRandom();
    private static final Cipher cipher = getCipher();

    private static Cipher getCipher() {
        try {
            return Cipher.getInstance("AES/CTR/NoPadding");
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    public static String encrypt(byte[] plaintext, SecretKey groupKey) {
        try {
            byte[] iv = new byte[16];
            SRAND.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, groupKey, ivspec);
            byte[] ciphertext = cipher.doFinal(plaintext);
            return Hex.encodeHexString(iv) + Hex.encodeHexString(ciphertext);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public static byte[] decrypt(String ciphertext, SecretKey groupKey) throws Exception {
        byte[] iv = DatatypeConverter.parseHexBinary(ciphertext.substring(0, 32));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, groupKey, ivParameterSpec);
        return cipher.doFinal(DatatypeConverter.parseHexBinary(ciphertext.substring(32)));
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
        try {
            if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.AES) {
                streamMessage.setEncryptionType(StreamMessage.EncryptionType.NONE);
                streamMessage.setSerializedContent(decrypt(streamMessage.getSerializedContent(), groupKey));
            } else if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.NEW_KEY_AND_AES) {
                byte[] plaintext = EncryptionUtil.decrypt(streamMessage.getSerializedContent(), groupKey);
                streamMessage.setEncryptionType(StreamMessage.EncryptionType.NONE);
                streamMessage.setSerializedContent(Arrays.copyOfRange(plaintext, 32, plaintext.length));
                return new SecretKeySpec(Arrays.copyOfRange(plaintext, 0, 32), "AES");
            }
        } catch (Exception e) {
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
}
