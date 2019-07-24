package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class KeyHistoryStorage extends KeyStorage {
    private HashMap<String, GroupKeyHistory> histories = new HashMap<>();

    public KeyHistoryStorage(HashMap<String, GroupKey> publisherGroupKeys) {
        super(publisherGroupKeys);
        for (String streamId: publisherGroupKeys.keySet()) {
            histories.put(streamId, new GroupKeyHistory(publisherGroupKeys.get(streamId)));
        }
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
    public List<GroupKey> getKeysBetween(String streamId, Date start, Date end) {
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
