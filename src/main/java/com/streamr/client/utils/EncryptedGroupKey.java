package com.streamr.client.utils;

import org.apache.commons.codec.binary.Hex;

import java.util.Date;
import java.util.Map;

public class EncryptedGroupKey extends GroupKey {
    public EncryptedGroupKey(String groupKeyHex, Date start) {
        super(groupKeyHex, start);
    }

    public UnencryptedGroupKey getDecrypted(EncryptionUtil encryptionUtil) {
        String plaintext = Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(groupKeyHex));
        return new UnencryptedGroupKey(plaintext, start);
    }

    public static EncryptedGroupKey fromMap(Map<String, Object> map) {
        return new EncryptedGroupKey((String) map.get("groupKey"), new Date(((Double) map.get("start")).longValue()));
    }
}
