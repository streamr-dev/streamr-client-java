package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class GroupKeyErrorResponse extends AbstractGroupKeyMessage {
    String requestId;
    String code;
    String message;

    public GroupKeyErrorResponse(String requestId, String streamId, String code, String message) {
        super(streamId);
        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(code, "code");
        ValidationUtil.checkNotNull(message, "message");

        this.requestId = requestId;
        this.code = code;
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static GroupKeyErrorResponse fromMap(Map<String, Object> map) {
        return new GroupKeyErrorResponse(
                (String) map.get("requestId"),
                (String) map.get("streamId"),
                (String) map.get("code"),
                (String) map.get("message")
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestId", requestId);
        map.put("streamId", streamId);
        map.put("code", code);
        map.put("message", message);
        return map;
    }
}
