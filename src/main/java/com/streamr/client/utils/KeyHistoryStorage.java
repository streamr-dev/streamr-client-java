package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/*
This key storage is used when the publisher wants to store all keys used to encrypt messages in order to
answer group key requests for historical keys.
 */
public class KeyHistoryStorage implements KeyStorage {
    private final HashMap<String, GroupKeyHistory> histories = new HashMap<>();

    public KeyHistoryStorage(HashMap<String, UnencryptedGroupKey> publisherGroupKeys) {
        super();
        for (String streamId: publisherGroupKeys.keySet()) {
            histories.put(streamId, new GroupKeyHistory(publisherGroupKeys.get(streamId)));
        }
    }

    public KeyHistoryStorage() {
        this(new HashMap<>());
    }

    @Override
    public boolean hasKey(String streamId) {
        return histories.containsKey(streamId);
    }

    @Override
    public UnencryptedGroupKey getLatestKey(String streamId) {
        GroupKeyHistory history = histories.get(streamId);
        return history == null ? null : history.getLatestKey();
    }

    @Override
    public ArrayList<UnencryptedGroupKey> getKeysBetween(String streamId, long start, long end) throws InvalidGroupKeyRequestException {
        GroupKeyHistory history = histories.get(streamId);
        return history == null ? new ArrayList<>() : history.getKeysBetween(start, end);
    }

    @Override
    public void addKey(String streamId, UnencryptedGroupKey key) {
        if (!histories.containsKey(streamId)) {
            histories.put(streamId, new GroupKeyHistory());
        }
        histories.get(streamId).addKey(key);
    }

    @Override
    public String toString() {
        return histories.toString();
    }
}
