package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;

public class GroupKeyHistory {
    ArrayList<GroupKey> keys = new ArrayList<>();
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

    public ArrayList<GroupKey> getKeysBetween(long start, long end) {
        if (start > end) {
            throw new IllegalArgumentException("'start' must be less or equal to 'end'");
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

    public ArrayList<GroupKey> getKeysBetween(Date start, Date end) {
        return getKeysBetween(start.getTime(), end.getTime());
    }

    public void addKey(GroupKey key) {
        keys.add(key);
    }

    private Long getKeyEnd(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= keys.size() - 1) {
            return null;
        }
        return keys.get(keyIndex + 1).getStartTime() - 1;
    }
}
