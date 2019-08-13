package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/*
This key storage is used when the publisher wants to store all keys used to encrypt messages in order to
answer group key requests for historical keys.
 */
public class KeyHistoryStorage extends KeyStorage {
    private HashMap<String, GroupKeyHistory> histories = new HashMap<>();

    public KeyHistoryStorage(HashMap<String, GroupKey> publisherGroupKeys) {
        super();
        for (String streamId: publisherGroupKeys.keySet()) {
            histories.put(streamId, new GroupKeyHistory(publisherGroupKeys.get(streamId)));
        }
    }

    public KeyHistoryStorage() {
        this(null);
    }

    @Override
    public boolean hasKey(String streamId) {
        return histories.containsKey(streamId);
    }

    @Override
    public GroupKey getLatestKey(String streamId) {
        GroupKeyHistory history = histories.get(streamId);
        return history == null ? null : history.getLatestKey();
    }

    @Override
    public ArrayList<GroupKey> getKeysBetween(String streamId, Date start, Date end) {
        GroupKeyHistory history = histories.get(streamId);
        return history == null ? new ArrayList<>() : history.getKeysBetween(start, end);
    }

    @Override
    public void addKey(String streamId, GroupKey key) {
        if (!histories.containsKey(streamId)) {
            histories.put(streamId, new GroupKeyHistory());
        }
        histories.get(streamId).addKey(key);
    }
}
