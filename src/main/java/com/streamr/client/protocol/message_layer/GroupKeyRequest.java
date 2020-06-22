package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class GroupKeyRequest extends AbstractGroupKeyMessage {
    private String requestId;
    private String publicKey;
    private Range range; // optional

    public GroupKeyRequest(String requestId, String streamId, String publicKey) {
        this(requestId, streamId, publicKey, null);
    }

    public GroupKeyRequest(String requestId, String streamId, String publicKey, Range range) {
        super(streamId);
        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(publicKey, "publicKey");

        this.requestId = requestId;
        this.publicKey = publicKey;
        this.range = range;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Range getRange() {
        return range;
    }

    public static GroupKeyRequest fromMap(Map<String, Object> map) {
        return new GroupKeyRequest(
                (String) map.get("requestId"),
                (String) map.get("streamId"),
                (String) map.get("publicKey"),
                map.containsKey("range") ? Range.fromMap((Map<String, Object>) map.get("range")) : null
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestId", requestId);
        map.put("streamId", streamId);
        map.put("publicKey", publicKey);
        map.put("range", range.toMap());
        return map;
    }

    public static class Range {
        private long start;
        private long end;

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public static Range fromMap(Map<String, Object> map) {
            return new Range(
                    ((Number) map.get("start")).longValue(),
                    ((Number) map.get("end")).longValue()
            );
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("start", start);
            map.put("end", end);
            return map;
        }
    }
}
