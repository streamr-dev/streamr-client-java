package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import java.security.SecureRandom;

public class GroupKey {

    private static final SecureRandom defaultSecureRandom = new SecureRandom();

    protected final String groupKeyId;
    protected final String groupKeyHex;

    private final SecretKey secretKey;

    public GroupKey(String groupKeyId, String groupKeyHex) throws InvalidGroupKeyException {
        this.groupKeyId = groupKeyId;
        this.groupKeyHex = groupKeyHex;
        secretKey = EncryptionUtil.getSecretKeyFromHexString(groupKeyHex);
    }

    public String getGroupKeyId() {
        return groupKeyId;
    }

    public String getGroupKeyHex() {
        return groupKeyHex;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupKey)) {
            return false;
        }
        GroupKey o = (GroupKey) other;
        return groupKeyHex.equals(o.groupKeyHex) && groupKeyId.equals(o.groupKeyId);
    }

    @Override
    public String toString() {
        return String.format("GroupKey{%s}", groupKeyId);
    }

    public static GroupKey generate() {
        return GroupKey.generate(defaultSecureRandom);
    }

    public static GroupKey generate(SecureRandom secureRandom) {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        try {
            return new GroupKey(IdGenerator.get(), Hex.encodeHexString(keyBytes));
        } catch (InvalidGroupKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
