package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.ValidationUtil;

import java.util.*;
import java.util.stream.Collectors;

public class GroupKeyResponse extends AbstractGroupKeyMessage {
    private String requestId;
    private Collection<Key> keys;

    public GroupKeyResponse(String requestId, String streamId, Collection<Key> keys) {
        super(streamId);

        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(keys, "keys");

        this.requestId = requestId;
        this.keys = keys;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Collection<Key> getKeys() {
        return keys;
    }

    public void setKeys(Collection<Key> keys) {
        this.keys = keys;
    }

    public static GroupKeyResponse fromMap(Map<String, Object> map) {
        List<Key> keys = new ArrayList<>();
        List<Map<String, Object>> keyMaps = (List<Map<String, Object>>) map.get("keys");

        if (keyMaps != null) {
            keys = keyMaps.stream()
                    .map(Key::fromMap)
                    .collect(Collectors.toList());
        }

        return new GroupKeyResponse(
                (String) map.get("requestId"),
                (String) map.get("streamId"),
                keys
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestId", requestId);
        map.put("streamId", streamId);
        map.put("keys", keys.stream().map(Key::toMap).collect(Collectors.toList()));
        return map;
    }

    public static class Key {
        private String groupKey;
        private long start;

        public Key(String groupKey, long start) {
            this.groupKey = groupKey;
            this.start = start;
        }

        public String getGroupKey() {
            return groupKey;
        }

        public long getStart() {
            return start;
        }

        public static Key fromMap(Map<String, Object> map) {
            return new Key(
                    (String) map.get("groupKey"),
                    ((Number) map.get("start")).longValue()
            );
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("groupKey", groupKey);
            map.put("start", start);
            return map;
        }

        public static Key fromGroupKey(GroupKey key) {
            return new Key(
                    key.getGroupKeyHex(),
                    key.getStartTime()
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (start != key.start) return false;
            return groupKey.equals(key.groupKey);
        }
    }
}
