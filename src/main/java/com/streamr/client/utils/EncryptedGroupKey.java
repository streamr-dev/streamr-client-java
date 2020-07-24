package com.streamr.client.utils;

/**
 * A container for encrypted group keys. Used to get compile-time checking
 * that encrypted and plaintext keys don't accidentally mix.
 */
public class EncryptedGroupKey {

    private final String groupKeyId;
    private final String encryptedGroupKeyHex;

    public EncryptedGroupKey(String groupKeyId, String encryptedGroupKeyHex) {
        this.groupKeyId = groupKeyId;
        this.encryptedGroupKeyHex = encryptedGroupKeyHex;
    }

    public String getGroupKeyId() {
        return groupKeyId;
    }

    public String getEncryptedGroupKeyHex() {
        return encryptedGroupKeyHex;
    }
}
