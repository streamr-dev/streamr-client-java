package com.streamr.client.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple GroupKeyStore implementation that stores the added keys in a HashMap.
 */
public class InMemoryGroupKeyStore extends GroupKeyStore {

    private final Map<String, Map<String, GroupKey>> keysByStreamId = new HashMap<>();
    private final Set<String> containedGroupKeyIds = new HashSet<>();

    @Override
    public GroupKey get(String streamId, String groupKeyId) {
        Map<String, GroupKey> keyByGroupKeyId = keysByStreamId.get(streamId);
        if (keyByGroupKeyId != null) {
            return keyByGroupKeyId.get(groupKeyId);
        }
        return null;
    }

    @Override
    public boolean contains(String groupKeyId) {
        return containedGroupKeyIds.contains(groupKeyId);
    }

    @Override
    public void storeKey(String streamId, GroupKey key) throws KeyAlreadyExistsException {
        Map<String, GroupKey> keyByGroupKeyId = keysByStreamId.get(streamId);
        if (keyByGroupKeyId == null) {
            keyByGroupKeyId = new HashMap<>();
            keysByStreamId.put(streamId, keyByGroupKeyId);
        }
        keyByGroupKeyId.put(key.getGroupKeyId(), key);
        containedGroupKeyIds.add(key.getGroupKeyId());
    }

    @Override
    public String toString() {
        return "InMemoryGroupKeyStore: " + keysByStreamId;
    }
}
