package com.streamr.client.protocol.message_layer;

import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.ValidationUtil;

import java.util.List;

public class GroupKeyAnnounce extends AbstractGroupKeyMessage {
    private List<GroupKey> groupKeys;

    public GroupKeyAnnounce(String streamId, List<GroupKey> groupKeys) {
        super(streamId);
        ValidationUtil.checkNotNull(groupKeys, "groupKeys");
        this.groupKeys = groupKeys;
    }

    public List<GroupKey> getGroupKeys() {
        return groupKeys;
    }

    @Override
    protected StreamMessage.MessageType getMessageType() {
        return StreamMessage.MessageType.GROUP_KEY_ANNOUNCE;
    }
}
