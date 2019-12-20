package com.streamr.client.utils;

import javax.crypto.SecretKey;
import java.util.Date;

public class UnencryptedGroupKey extends GroupKey {
    private final SecretKey secretKey;

    public UnencryptedGroupKey(String groupKeyHex, Date start) {
        super (groupKeyHex, start);
        secretKey = EncryptionUtil.getSecretKeyFromHexString(groupKeyHex);
    }

    public UnencryptedGroupKey(String groupKeyHex) {
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
