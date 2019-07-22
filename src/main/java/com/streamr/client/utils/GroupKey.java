package com.streamr.client.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroupKey {
    private String groupKeyHex;
    private Date start;

    // "groupKeyHex" might be encrypted or not.
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

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("groupKey", groupKeyHex);
        map.put("start", getStartTime());
        return map;
    }
}
