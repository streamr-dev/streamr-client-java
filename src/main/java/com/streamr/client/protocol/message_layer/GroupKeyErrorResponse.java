package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.List;

public class GroupKeyErrorResponse extends AbstractGroupKeyMessage {
    private final String requestId;
    private final String code;
    private final String message;
    private final List<String> groupKeyIds;

    public GroupKeyErrorResponse(String requestId, String streamId, String code, String message, List<String> groupKeyIds) {
        super(streamId);
        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(code, "code");
        ValidationUtil.checkNotNull(message, "message");
        ValidationUtil.checkNotNull(groupKeyIds, "groupKeyIds");
        ValidationUtil.checkNotEmpty(groupKeyIds, "groupKeyIds");

        this.requestId = requestId;
        this.code = code;
        this.message = message;
        this.groupKeyIds = groupKeyIds;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getGroupKeyIds() {
        return groupKeyIds;
    }

    @Override
    protected StreamMessage.MessageType getMessageType() {
        return StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupKeyErrorResponse that = (GroupKeyErrorResponse) o;

        if (!streamId.equals(that.streamId)) return false;
        if (!requestId.equals(that.requestId)) return false;
        if (!code.equals(that.code)) return false;
        if (!message.equals(that.message)) return false;
        return groupKeyIds.equals(that.groupKeyIds);
    }

    @Override
    public String toString() {
        return String.format("GroupKeyErrorResponse{requestId=%s, streamId=%s, code=%s, message=%s}", requestId, streamId, code, message);
    }
}
