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
}
