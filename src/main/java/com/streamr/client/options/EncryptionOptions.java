package com.streamr.client.options;

import java.util.HashMap;

public class EncryptionOptions {
    private HashMap<String, String> publisherGroupKeys; // streamId --> groupKeyHex
    private HashMap<String, HashMap<String, String>> subscriberGroupKeys; // streamId --> (publisherId --> groupKeyHex)

    public EncryptionOptions() {
        this.publisherGroupKeys = new HashMap<>();
        this.subscriberGroupKeys = new HashMap<>();
    }

    public EncryptionOptions(HashMap<String, String> publisherGroupKeys, HashMap<String, HashMap<String, String>> subscriberGroupKeys) {
        this.publisherGroupKeys = publisherGroupKeys;
        this.subscriberGroupKeys = subscriberGroupKeys;
    }

    public HashMap<String, String> getPublisherGroupKeys() {
        return publisherGroupKeys;
    }

    public HashMap<String, HashMap<String, String>> getSubscriberGroupKeys() {
        return subscriberGroupKeys;
    }

    public static EncryptionOptions getDefault() {
        return new EncryptionOptions();
    }
}
