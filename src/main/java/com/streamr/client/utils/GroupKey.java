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

    public String getGroupKeyHex() {
        return groupKeyHex;
    }

    public long getStartTime() {
        return start.getTime();
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
