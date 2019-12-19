package com.streamr.client.options;

import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKey;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

public class EncryptionOptions {
    private final HashMap<String, GroupKey> publisherGroupKeys; // streamId --> groupKeyHex
    private final HashMap<String, HashMap<String, GroupKey>> subscriberGroupKeys; // streamId --> (publisherId --> groupKeyHex)
    private boolean publisherStoreKeyHistory = true;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;

    public EncryptionOptions(HashMap<String, GroupKey> publisherGroupKeys, HashMap<String, HashMap<String, GroupKey>> subscriberGroupKeys,
                             boolean publisherStoreKeyHistory, String rsaPublicKey, String rsaPrivateKey) {
        this.publisherGroupKeys = publisherGroupKeys;
        this.subscriberGroupKeys = subscriberGroupKeys;
        this.publisherStoreKeyHistory = publisherStoreKeyHistory;
        if (rsaPublicKey != null) {
            EncryptionUtil.validatePublicKey(rsaPublicKey);
            this.rsaPublicKey = EncryptionUtil.getPublicKeyFromString(rsaPublicKey);
        }
        if (rsaPrivateKey != null) {
            EncryptionUtil.validatePrivateKey(rsaPrivateKey);
            this.rsaPrivateKey = EncryptionUtil.getPrivateKeyFromString(rsaPrivateKey);
        }
    }

    public EncryptionOptions(HashMap<String, GroupKey> publisherGroupKeys, HashMap<String, HashMap<String, GroupKey>> subscriberGroupKeys) {
        this(publisherGroupKeys, subscriberGroupKeys, true, null, null);
    }

    public EncryptionOptions() {
        this(new HashMap<>(), new HashMap<>(), true, null, null);
    }

    public HashMap<String, GroupKey> getPublisherGroupKeys() {
        return publisherGroupKeys;
    }

    public HashMap<String, HashMap<String, GroupKey>> getSubscriberGroupKeys() {
        return subscriberGroupKeys;
    }

    public boolean getPublisherStoreKeyHistory() {
        return publisherStoreKeyHistory;
    }

    public RSAPublicKey getRsaPublicKey() {
        return rsaPublicKey;
    }

    public RSAPrivateKey getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public static EncryptionOptions getDefault() {
        return new EncryptionOptions();
    }
}
