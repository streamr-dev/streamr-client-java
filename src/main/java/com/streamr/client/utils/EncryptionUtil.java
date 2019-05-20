package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.security.SecureRandom;

public class EncryptionUtil {
    private static final Logger log = LogManager.getLogger();
    private static final SecureRandom SRAND = new SecureRandom();

    public static String encrypt(byte[] plaintext, SecretKey groupKey) {
        try {
            Cipher encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            byte[] iv = new byte[16];
            SRAND.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            encryptCipher.init(Cipher.ENCRYPT_MODE, groupKey, ivspec);
            byte[] ciphertext = encryptCipher.doFinal(plaintext);
            return Hex.encodeHexString(iv) + Hex.encodeHexString(ciphertext);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    public static byte[] decrypt(String ciphertext, SecretKey groupKey) throws Exception {
        Cipher decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
        byte[] iv = DatatypeConverter.parseHexBinary(ciphertext.substring(0, 32));
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, groupKey, ivParameterSpec);
        return decryptCipher.doFinal(DatatypeConverter.parseHexBinary(ciphertext.substring(32)));
    }

    public static void validateGroupKey(String groupKeyHex) {
        String without0x = groupKeyHex.startsWith("0x") ? groupKeyHex.substring(2) : groupKeyHex;
        if (without0x.length() != 64) { // the key must be 256 bits long
            throw new InvalidGroupKeyException(without0x.length() * 4);
        }
    }
}
