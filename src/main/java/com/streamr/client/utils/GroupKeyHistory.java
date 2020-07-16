package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyRequestException;

import java.util.ArrayList;

/*
This class contains the history of keys used to publish encrypted messages. The different methods are
used to create encrypted messages and to answer group key requests from subscribers.
 */
public class GroupKeyHistory {
    private final ArrayList<GroupKey> keys = new ArrayList<>();
    public GroupKeyHistory() {

    }
    public GroupKeyHistory(GroupKey initialGroupKey) {
        keys.add(initialGroupKey);
    }

    public GroupKey getLatestKey() {
        if (keys.isEmpty()) {
            return null;
        }
        return keys.get(keys.size() - 1);
    }

    public ArrayList<GroupKey> getKeysBetween(long start, long end) throws InvalidGroupKeyRequestException {
        if (start > end) {
            throw new InvalidGroupKeyRequestException("'start' must be less or equal to 'end'");
        }
        int i = 0;
        // discard keys that ended before 'start'
        while (i < keys.size() - 1 && this.getKeyEnd(i) < start) {
            i++;
        }
        ArrayList<GroupKey> selected = new ArrayList<>();
        // add keys as long as they started before 'end'
        while (i < keys.size() && keys.get(i).getStartTime() <= end) {
            selected.add(keys.get(i));
            i++;
        }
        return selected;
    }

    public void addKey(GroupKey key) {
        GroupKey latestKey = keys.size() == 0 ? null : keys.get(keys.size() - 1);
        if (latestKey != null && latestKey.start.getTime() > key.start.getTime()) {
            throw new IllegalArgumentException("Trying to add a key older than the latest key (" + key.start + " < " + latestKey.start);
        }
        keys.add(key);
    }

    private Long getKeyEnd(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= keys.size() - 1) {
            return null;
        }
        return keys.get(keyIndex + 1).getStartTime() - 1;
    }
}
