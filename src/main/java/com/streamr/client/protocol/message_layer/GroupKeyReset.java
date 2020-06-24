package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class GroupKeyReset extends AbstractGroupKeyMessage {
    private String groupKey;
    private long start;

    public GroupKeyReset(String streamId, String groupKey, long start) {
        super(streamId);
        ValidationUtil.checkNotNull(groupKey, "groupKey");

        this.groupKey = groupKey;
        this.start = start;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public static GroupKeyReset fromMap(Map<String, Object> map) {
        return new GroupKeyReset(
                (String) map.get("streamId"),
                (String) map.get("groupKey"),
                ((Number) map.get("start")).longValue()
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("streamId", streamId);
        map.put("groupKey", groupKey);
        map.put("start", start);
        return map;
    }
}
