package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.ValidationUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupKeyRequest extends AbstractGroupKeyMessage {
    private String requestId;
    private String publicKey;
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
}
