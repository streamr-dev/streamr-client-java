package com.streamr.client.options;

import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.UnencryptedGroupKey;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

public class EncryptionOptions {
    private final HashMap<String, UnencryptedGroupKey> publisherGroupKeys; // streamId --> groupKeyHex
    private final HashMap<String, HashMap<String, UnencryptedGroupKey>> subscriberGroupKeys; // streamId --> (publisherId --> groupKeyHex)
    private boolean publisherStoreKeyHistory = true;
    private RSAPublicKey rsaPublicKey;
    private RSAPrivateKey rsaPrivateKey;
    private boolean autoRevoke = true;

    public EncryptionOptions(HashMap<String, UnencryptedGroupKey> publisherGroupKeys, HashMap<String, HashMap<String, UnencryptedGroupKey>> subscriberGroupKeys,
                             boolean publisherStoreKeyHistory, String rsaPublicKey, String rsaPrivateKey, boolean autoRevoke) {
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
        this.autoRevoke = autoRevoke;
    }

    public EncryptionOptions(HashMap<String, UnencryptedGroupKey> publisherGroupKeys, HashMap<String, HashMap<String, UnencryptedGroupKey>> subscriberGroupKeys,
                             boolean publisherStoreKeyHistory, String rsaPublicKey, String rsaPrivateKey) {
        this(publisherGroupKeys, subscriberGroupKeys, publisherStoreKeyHistory, rsaPublicKey, rsaPrivateKey, true);
    }

    public EncryptionOptions(HashMap<String, UnencryptedGroupKey> publisherGroupKeys, HashMap<String, HashMap<String, UnencryptedGroupKey>> subscriberGroupKeys) {
        this(publisherGroupKeys, subscriberGroupKeys, true, null, null, true);
    }

    public EncryptionOptions() {
        this(new HashMap<>(), new HashMap<>(), true, null, null);
    }

    public HashMap<String, UnencryptedGroupKey> getPublisherGroupKeys() {
        return publisherGroupKeys;
    }

    public HashMap<String, HashMap<String, UnencryptedGroupKey>> getSubscriberGroupKeys() {
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

    public boolean autoRevoke() {
        return autoRevoke;
    }

    public static EncryptionOptions getDefault() {
        return new EncryptionOptions();
    }
}
