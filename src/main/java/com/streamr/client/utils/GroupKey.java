package com.streamr.client.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class GroupKey {
    protected final String groupKeyHex;
    protected final Date start;

    public GroupKey(String groupKeyHex, Date start) {
        this.groupKeyHex = groupKeyHex;
        this.start = start;
    }

    public GroupKey(String groupKeyHex, long start) {
        this(groupKeyHex, new Date(start));
    }

    public GroupKey(String groupKeyHex) {
        this(groupKeyHex, new Date());
    }

    public String getGroupKeyHex() {
        return groupKeyHex;
    }

    public long getStartTime() {
        return start.getTime();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("groupKey", groupKeyHex);
        map.put("start", getStartTime());
        return map;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GroupKey)) {
            return false;
        }
        GroupKey o = (GroupKey) other;
        return groupKeyHex.equals(o.groupKeyHex) && start.equals(o.start);
    }
}
