package com.streamr.client.utils;

import java.util.ArrayList;
import java.util.Date;

/*
This class contains the history of keys used to publish encrypted messages. The different methods are
used to create encrypted messages and to answer group key requests from subscribers.
 */
public class GroupKeyHistory {
    private final ArrayList<UnencryptedGroupKey> keys = new ArrayList<>();
    public GroupKeyHistory() {

    }
    public GroupKeyHistory(UnencryptedGroupKey initialGroupKey) {
        keys.add(initialGroupKey);
    }

    public UnencryptedGroupKey getLatestKey() {
        if (keys.isEmpty()) {
            return null;
        }
        return keys.get(keys.size() - 1);
    }

    public ArrayList<UnencryptedGroupKey> getKeysBetween(long start, long end) {
        if (start > end) {
            throw new IllegalArgumentException("'start' must be less or equal to 'end'");
        }
        int i = 0;
        // discard keys that ended before 'start'
        while (i < keys.size() - 1 && this.getKeyEnd(i) < start) {
            i++;
        }
        ArrayList<UnencryptedGroupKey> selected = new ArrayList<>();
        // add keys as long as they started before 'end'
        while (i < keys.size() && keys.get(i).getStartTime() <= end) {
            selected.add(keys.get(i));
            i++;
        }
        return selected;
    }

    public ArrayList<UnencryptedGroupKey> getKeysBetween(Date start, Date end) {
        return getKeysBetween(start.getTime(), end.getTime());
    }

    public void addKey(UnencryptedGroupKey key) {
        keys.add(key);
    }

    private Long getKeyEnd(int keyIndex) {
        if (keyIndex < 0 || keyIndex >= keys.size() - 1) {
            return null;
        }
        return keys.get(keyIndex + 1).getStartTime() - 1;
    }
}
