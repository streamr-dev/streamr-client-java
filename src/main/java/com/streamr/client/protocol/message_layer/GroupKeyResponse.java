package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.ValidationUtil;

import java.util.List;

public class GroupKeyResponse extends AbstractGroupKeyMessage {
    private final String requestId;
    private final List<GroupKey> keys;

    public GroupKeyResponse(String requestId, String streamId, List<GroupKey> keys) {
        super(streamId);

        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(keys, "keys");
        ValidationUtil.checkNotEmpty(keys, "keys");

        this.requestId = requestId;
        this.keys = keys;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<GroupKey> getKeys() {
        return keys;
    }

    @Override
    protected StreamMessage.MessageType getMessageType() {
        return StreamMessage.MessageType.GROUP_KEY_RESPONSE;
    }
}
