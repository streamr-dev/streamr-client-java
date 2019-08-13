package com.streamr.client.utils;

import com.streamr.client.exceptions.EncryptedGroupKeyException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroupKey {
    private final String groupKeyHex;
    private final boolean isEncrypted;
    private final SecretKey secretKey;
    private final Date start;

    // "groupKeyHex" might be encrypted or in plaintext.
    public GroupKey(String groupKeyHex, Date start, boolean isEncrypted) {
        this.groupKeyHex = groupKeyHex;
        this.isEncrypted = isEncrypted;
        if (!isEncrypted) {
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
        } else {
            secretKey = null;
        }
        this.start = start;
    }

    public GroupKey(String groupKeyHex, Date start) {
        this(groupKeyHex, start, false);
    }

    public GroupKey(String groupKeyHex, long start) {
        this(groupKeyHex, new Date(start));
    }

    public GroupKey(String groupKeyHex, long start, boolean isEncrypted) {
        this(groupKeyHex, new Date(start), isEncrypted);
    }

    public GroupKey(String groupKeyHex) {
        this(groupKeyHex, new Date());
    }

    public String getGroupKeyHex() {
        return groupKeyHex;
    }

    public long getStartTime() {
        return start.getTime();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("groupKey", groupKeyHex);
        map.put("start", getStartTime());
        return map;
    }

    public GroupKey getEncrypted(String publicKey) {
        String ciphertext = EncryptionUtil.encryptWithPublicKey(groupKeyHex, publicKey);
        return new GroupKey(ciphertext, start, true);
    }

    public GroupKey getDecrypted(EncryptionUtil encryptionUtil) {
        String plaintext = Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(groupKeyHex));
        return new GroupKey(plaintext, start);
    }

    /*
    If this GroupKey is not encrypted, returns the object used for encryption/decryption.
     */
    public SecretKey getSecretKey() {
        if (isEncrypted) {
            throw new EncryptedGroupKeyException();
        }
        return secretKey;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupKey)) {
            return false;
        }
        GroupKey o = (GroupKey) other;
        return groupKeyHex.equals(o.groupKeyHex) && start.equals(o.start);
    }

    public static GroupKey fromMap(Map<String, Object> map, boolean isEncrypted) {
        return new GroupKey((String) map.get("groupKey"), ((Double) map.get("start")).longValue(), isEncrypted);
    }
}
