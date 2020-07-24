package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.EncryptedGroupKey;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.ValidationUtil;

import java.util.List;

public class GroupKeyResponse extends AbstractGroupKeyMessage {
    private final String requestId;
    private final List<EncryptedGroupKey> keys;

    public GroupKeyResponse(String requestId, String streamId, List<EncryptedGroupKey> keys) {
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

    public List<EncryptedGroupKey> getKeys() {
        return keys;
    }

    @Override
    protected StreamMessage.MessageType getMessageType() {
        return StreamMessage.MessageType.GROUP_KEY_RESPONSE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupKeyResponse that = (GroupKeyResponse) o;

        if (!requestId.equals(that.requestId)) return false;
        if (!streamId.equals(that.streamId)) return false;
        return keys.equals(that.keys);
    }
}
