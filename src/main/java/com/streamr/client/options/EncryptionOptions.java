package com.streamr.client.options;

import com.streamr.client.utils.GroupKey;

import java.util.HashMap;

public class EncryptionOptions {
    private HashMap<String, GroupKey> publisherGroupKeys; // streamId --> groupKeyHex
    private HashMap<String, HashMap<String, GroupKey>> subscriberGroupKeys; // streamId --> (publisherId --> groupKeyHex)
    private boolean publisherStoreKeyHistory = true;
    private String rsaPublicKey; // in the PEM format
    private String rsaPrivateKey; // in the PEM format

    public EncryptionOptions() {
        this.publisherGroupKeys = new HashMap<>();
        this.subscriberGroupKeys = new HashMap<>();
    }

    public EncryptionOptions(HashMap<String, GroupKey> publisherGroupKeys, HashMap<String, HashMap<String, GroupKey>> subscriberGroupKeys,
                             boolean publisherStoreKeyHistory, String rsaPublicKey, String rsaPrivateKey) {
        this.publisherGroupKeys = publisherGroupKeys;
        this.subscriberGroupKeys = subscriberGroupKeys;
        this.publisherStoreKeyHistory = publisherStoreKeyHistory;
        this.rsaPublicKey = rsaPublicKey;
        this.rsaPrivateKey = rsaPrivateKey;
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

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public String getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public static EncryptionOptions getDefault() {
        return new EncryptionOptions();
    }
}
