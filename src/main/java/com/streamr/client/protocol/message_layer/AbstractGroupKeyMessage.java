package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.Map;

public abstract class AbstractGroupKeyMessage {
    protected final String streamId;

    public AbstractGroupKeyMessage(String streamId) {
        ValidationUtil.checkNotNull(streamId, "streamId");
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    public static AbstractGroupKeyMessage fromContent(Map<String, Object> content, StreamMessage.MessageType messageType) {
        switch (messageType) {
            case GROUP_KEY_REQUEST:
                return GroupKeyRequest.fromMap(content);
            case GROUP_KEY_RESPONSE_SIMPLE:
                return GroupKeyResponse.fromMap(content);
            case GROUP_KEY_RESET_SIMPLE:
                return GroupKeyReset.fromMap(content);
            case GROUP_KEY_RESPONSE_ERROR:
                return GroupKeyErrorResponse.fromMap(content);
            default:
                throw new RuntimeException("MessageType can not be converted to a group key message: " + messageType);
        }
    }

}
