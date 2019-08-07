package com.streamr.client.utils;

import org.apache.commons.codec.binary.Hex;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroupKey {
    private String groupKeyHex;
    private Date start;

    // "groupKeyHex" might be encrypted or not.
    public GroupKey(String groupKeyHex, Date start) {
        this.groupKeyHex = groupKeyHex;
        this.start = start;
    }

    public GroupKey(String groupKeyHex, long start) {
        this(groupKeyHex, new Date(start));
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
        return new GroupKey(ciphertext, start);
    }

    public GroupKey getDecrypted(EncryptionUtil encryptionUtil) {
        String plaintext = Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(groupKeyHex));
        return new GroupKey(plaintext, start);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupKey)) {
            return false;
        }
        GroupKey o = (GroupKey) other;
        return groupKeyHex.equals(o.groupKeyHex) && start.equals(o.start);
    }

    public static GroupKey fromMap(Map<String, Object> map) {
        return new GroupKey((String) map.get("groupKey"), (long) map.get("start"));
    }
}
