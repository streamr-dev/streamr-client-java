package com.streamr.client.protocol.message_layer;

import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.utils.ValidationUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGroupKeyMessage {
    protected final String streamId;

    private static final Map<StreamMessage.MessageType, AbstractGroupKeyMessageAdapter<? extends AbstractGroupKeyMessage>> adapterByMessageType = new HashMap<>();

    static {
        adapterByMessageType.put(StreamMessage.MessageType.GROUP_KEY_REQUEST, new GroupKeyRequestAdapter());
        adapterByMessageType.put(StreamMessage.MessageType.GROUP_KEY_RESPONSE, new GroupKeyResponseAdapter());
        adapterByMessageType.put(StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE, new GroupKeyErrorResponseAdapter());
        adapterByMessageType.put(StreamMessage.MessageType.GROUP_KEY_ANNOUNCE, new GroupKeyAnnounceAdapter());
    }

    public AbstractGroupKeyMessage(String streamId) {
        ValidationUtil.checkNotNull(streamId, "streamId");
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    protected abstract StreamMessage.MessageType getMessageType();

    public String serialize() {
        return adapterByMessageType.get(getMessageType()).groupKeyMessageToJson(this);
    }

    public static AbstractGroupKeyMessage deserialize(String serialized, StreamMessage.MessageType messageType) {
        try {
            return adapterByMessageType.get(messageType).fromJson(serialized);
        } catch (IOException e) {
            throw new MalformedMessageException("Failed to parse GroupKeyMessage: " + serialized + " as messageType " + messageType, e);
        }
    }

    public static AbstractGroupKeyMessage fromStreamMessage(StreamMessage streamMessage) throws MalformedMessageException {
        return AbstractGroupKeyMessage.deserialize(streamMessage.getSerializedContent(), streamMessage.getMessageType());
    }

    public StreamMessage toStreamMessage(MessageID messageID, MessageRef prevMsgRef) {
        return new StreamMessage(
                messageID,
                prevMsgRef,
                getMessageType(),
                serialize(),
                StreamMessage.ContentType.JSON,
                StreamMessage.EncryptionType.NONE,
                null,
                null,
                StreamMessage.SignatureType.NONE,
                null);
    }

}
