package com.streamr.client.utils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

public class UnencryptedGroupKey extends GroupKey {
    private final SecretKey secretKey;

    public UnencryptedGroupKey(String groupKeyHex, Date start) {
        super (groupKeyHex, start);
        EncryptionUtil.validateGroupKey(groupKeyHex);
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, false);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
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
