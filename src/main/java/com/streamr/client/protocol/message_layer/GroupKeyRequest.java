package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.List;

public class GroupKeyRequest extends AbstractGroupKeyMessage {
    private final String requestId;
    private final String publicKey;
    private final List<String> groupKeyIds;

    public GroupKeyRequest(String requestId, String streamId, String rsaPublicKey, List<String> groupKeyIds) {
        super(streamId);
        ValidationUtil.checkNotNull(requestId, "requestId");
        ValidationUtil.checkNotNull(rsaPublicKey, "rsaPublicKey");
        ValidationUtil.checkNotNull(groupKeyIds, "groupKeyIds");
        ValidationUtil.checkNotEmpty(groupKeyIds, "groupKeyIds");

        this.requestId = requestId;
        this.publicKey = rsaPublicKey;
        this.groupKeyIds = groupKeyIds;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<String> getGroupKeyIds() {
        return groupKeyIds;
    }

    @Override
    protected StreamMessage.MessageType getMessageType() {
        return StreamMessage.MessageType.GROUP_KEY_REQUEST;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupKeyRequest that = (GroupKeyRequest) o;

        if (!requestId.equals(that.requestId)) return false;
        if (!streamId.equals(that.streamId)) return false;
        if (!publicKey.equals(that.publicKey)) return false;
        return groupKeyIds.equals(that.groupKeyIds);
    }

    @Override
    public String toString() {
        return String.format("GroupKeyRequest{requestId=%s, streamId=%s, keys=%s, publicKey=%s}", requestId, streamId, groupKeyIds, publicKey);
    }
}
