package com.streamr.client.protocol.message_layer;

import com.streamr.client.java.util.Objects;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage.MessageType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGroupKeyMessage {
  protected final String streamId;

  private static final Map<
          MessageType, AbstractGroupKeyMessageAdapter<? extends AbstractGroupKeyMessage>>
      adapterByMessageType = new HashMap<>();

  static {
    adapterByMessageType.put(MessageType.GROUP_KEY_REQUEST, new GroupKeyRequestAdapter());
    adapterByMessageType.put(MessageType.GROUP_KEY_RESPONSE, new GroupKeyResponseAdapter());
    adapterByMessageType.put(
        MessageType.GROUP_KEY_ERROR_RESPONSE, new GroupKeyErrorResponseAdapter());
    adapterByMessageType.put(MessageType.GROUP_KEY_ANNOUNCE, new GroupKeyAnnounceAdapter());
  }

  public AbstractGroupKeyMessage(String streamId) {
    Objects.requireNonNull(streamId, "streamId");
    this.streamId = streamId;
  }

  public String getStreamId() {
    return streamId;
  }

  protected abstract MessageType getMessageType();

  public String serialize() {
    return adapterByMessageType.get(getMessageType()).groupKeyMessageToJson(this);
  }

  public static AbstractGroupKeyMessage deserialize(String serialized, MessageType messageType) {
    try {
      return adapterByMessageType.get(messageType).fromJson(serialized);
    } catch (final IOException e) {
      final String message =
          String.format(
              "Failed to parse GroupKeyMessage: %s as messageType %s", serialized, messageType);
      throw new MalformedMessageException(message, e);
    }
  }

  public static AbstractGroupKeyMessage fromStreamMessage(StreamMessage streamMessage)
      throws MalformedMessageException {
    return AbstractGroupKeyMessage.deserialize(
        streamMessage.getSerializedContent(), streamMessage.getMessageType());
  }

  public StreamMessage.Builder toStreamMessageBuilder(
      final MessageId messageId, final MessageRef prevMsgRef) {
    final StreamMessage.Content content =
        StreamMessage.Content.Factory.withJsonAsPayload(serialize());
    return new StreamMessage.Builder()
        .withMessageId(messageId)
        .withPreviousMessageRef(prevMsgRef)
        .withMessageType(getMessageType())
        .withContent(content)
        .withEncryptionType(StreamMessage.EncryptionType.NONE)
        .withGroupKeyId(null)
        .withNewGroupKey(null)
        .withSignatureType(StreamMessage.SignatureType.NONE)
        .withSignature(null);
  }
}
