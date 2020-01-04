package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;

import javax.crypto.SecretKey;
import java.util.Date;

public class UnencryptedGroupKey extends GroupKey {
    private final SecretKey secretKey;

    public UnencryptedGroupKey(String groupKeyHex, Date start) throws InvalidGroupKeyException {
        super (groupKeyHex, start);
        secretKey = EncryptionUtil.getSecretKeyFromHexString(groupKeyHex);
    }

    public UnencryptedGroupKey(String groupKeyHex) throws InvalidGroupKeyException {
        this(groupKeyHex, new Date());
    }

    public EncryptedGroupKey getEncrypted(String publicKey) {
        String ciphertext = EncryptionUtil.encryptWithPublicKey(groupKeyHex, publicKey);
        return new EncryptedGroupKey(ciphertext, start);
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
