package com.streamr.client.protocol.message_layer;

import com.streamr.client.java.util.Objects;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.common.UnsupportedMessageException;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.EncryptedGroupKey;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import okio.Buffer;

public final class StreamMessage implements Serializable {
  public static final int LATEST_VERSION = 32;

  public enum MessageType {
    STREAM_MESSAGE((byte) 27),
    GROUP_KEY_REQUEST((byte) 28),
    GROUP_KEY_RESPONSE((byte) 29),
    GROUP_KEY_ANNOUNCE((byte) 30),
    GROUP_KEY_ERROR_RESPONSE((byte) 31);

    private final byte id;

    MessageType(final byte id) {
      this.id = id;
    }

    public byte getId() {
      return this.id;
    }

    public static MessageType fromId(final byte id) {
      if (id == STREAM_MESSAGE.id) {
        return STREAM_MESSAGE;
      } else if (id == GROUP_KEY_REQUEST.id) {
        return GROUP_KEY_REQUEST;
      } else if (id == GROUP_KEY_RESPONSE.id) {
        return GROUP_KEY_RESPONSE;
      } else if (id == GROUP_KEY_ANNOUNCE.id) {
        return GROUP_KEY_ANNOUNCE;
      } else if (id == GROUP_KEY_ERROR_RESPONSE.id) {
        return GROUP_KEY_ERROR_RESPONSE;
      }
      throw new UnsupportedMessageException("Unrecognized content type: " + id);
    }
  }

  public enum SignatureType {
    NONE((byte) 0),
    ETH_LEGACY((byte) 1),
    ETH((byte) 2);

    private final byte id;

    SignatureType(final byte id) {
      this.id = id;
    }

    public byte getId() {
      return this.id;
    }

    public static SignatureType fromId(final byte id) {
      if (id == NONE.id) {
        return NONE;
      } else if (id == ETH_LEGACY.id) {
        return ETH_LEGACY;
      } else if (id == ETH.id) {
        return ETH;
      }
      throw new UnsupportedMessageException("Unrecognized signature type: " + id);
    }
  }

  public enum EncryptionType {
    NONE((byte) 0),
    RSA((byte) 1),
    AES((byte) 2);

    private final byte id;

    EncryptionType(final byte id) {
      this.id = id;
    }

    public byte getId() {
      return this.id;
    }

    public static EncryptionType fromId(final byte id) {
      if (id == NONE.id) {
        return NONE;
      } else if (id == RSA.id) {
        return RSA;
      } else if (id == AES.id) {
        return AES;
      }
      throw new UnsupportedMessageException("Unrecognized encryption type: " + id);
    }
  }

  public static final class Content {
    private final Type type;
    private final byte[] payload;

    private final ContentAdapter adapter = new ContentAdapter();
    private Map<String, Object> cache = Collections.unmodifiableMap(new HashMap<>());

    private Content(final Type type, final byte[] payload) {
      Objects.requireNonNull(type);
      this.type = type;
      Objects.requireNonNull(payload);
      this.payload = payload;
    }

    public Map<String, Object> toMap() {
      if (type != Type.JSON) {
        throw new RuntimeException("Unknown contentType encountered: " + type);
      }
      try {
        parseContentCache();
      } catch (final IOException e) {
        throw new RuntimeException("Failed to parse message content: " + toString());
      }
      return cache;
    }

    public void parseContentCache() throws IOException {
      try (final Buffer buffer = new Buffer()) {
        buffer.write(payload);
        cache = adapter.fromJson(buffer);
      }
    }

    @Override
    public String toString() {
      return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      final Content content = (Content) obj;
      return type == content.type && Arrays.equals(payload, content.payload);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(type);
      result = 31 * result + Arrays.hashCode(payload);
      return result;
    }

    public enum Type {
      JSON((byte) 0);

      private final byte id;

      Type(final byte id) {
        this.id = id;
      }

      public byte getId() {
        return this.id;
      }

      public static Type fromId(final byte id) {
        if (id == JSON.id) {
          return JSON;
        }
        throw new UnsupportedMessageException("Unrecognized ContentType: " + id);
      }
    }

    public static class Builder {
      private Type type = Type.JSON;
      private byte[] payload = new byte[0];

      public Builder() {}

      public Builder(final Content content) {
        Objects.requireNonNull(content);
        this.payload = content.payload;
        this.type = content.type;
      }

      public StreamMessage.Content.Builder withContentType(final Type contentType) {
        this.type = contentType;
        return this;
      }

      public StreamMessage.Content.Builder withPayload(final byte[] payload) {
        Objects.requireNonNull(payload);
        this.payload = payload;
        return this;
      }

      public Content createContent() {
        return new Content(type, payload);
      }
    }

    public static class Factory {
      public static StreamMessage.Content withJsonAsPayload(final String payload) {
        return withJsonAsPayload(payload.getBytes(StandardCharsets.UTF_8));
      }

      public static StreamMessage.Content withJsonAsPayload(final byte[] payload) {
        return new Content.Builder()
            .withContentType(Type.JSON)
            .withPayload(payload)
            .createContent();
      }
    }
  }

  private final MessageId messageId;
  private final MessageRef previousMessageRef;
  private final MessageType messageType;
  private final Content content;
  private final EncryptionType encryptionType;
  private final String groupKeyId;
  private final EncryptedGroupKey newGroupKey;
  private final SignatureType signatureType;
  private final String signature;

  /**
   * Full constructor, creates a StreamMessage with all fields directly set to the provided values.
   */
  private StreamMessage(
      final MessageId messageId,
      final MessageRef previousMessageRef,
      final MessageType messageType,
      final Content content,
      final EncryptionType encryptionType,
      final String groupKeyId,
      final EncryptedGroupKey newGroupKey,
      final SignatureType signatureType,
      final String signature) {
    this.messageId = messageId;
    this.previousMessageRef = previousMessageRef;
    this.messageType = messageType;
    Objects.requireNonNull(content);
    this.content = content;
    this.encryptionType = encryptionType;
    this.groupKeyId = groupKeyId;
    this.newGroupKey = newGroupKey;
    this.signatureType = signatureType;
    this.signature = signature;
  }

  public MessageId getMessageId() {
    return messageId;
  }

  public String getStreamId() {
    return messageId.getStreamId();
  }

  public int getStreamPartition() {
    return messageId.getStreamPartition();
  }

  public long getTimestamp() {
    return messageId.getTimestamp();
  }

  public long getSequenceNumber() {
    return messageId.getSequenceNumber();
  }

  public Address getPublisherId() {
    return messageId.getPublisherId();
  }

  public String getMsgChainId() {
    return messageId.getMsgChainId();
  }

  public MessageRef getPreviousMessageRef() {
    return previousMessageRef;
  }

  public SignatureType getSignatureType() {
    return signatureType;
  }

  public String getSignature() {
    return signature;
  }

  public MessageRef getMessageRef() {
    return new MessageRef(getTimestamp(), getSequenceNumber());
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public Content.Type getContentType() {
    return content.type;
  }

  public EncryptionType getEncryptionType() {
    return encryptionType;
  }

  // Soon @Deprecated almost exclusively used by tests
  public Map<String, Object> getParsedContent() {
    if (encryptionType != EncryptionType.NONE) {
      throw new EncryptedContentNotParsableException(encryptionType);
    }
    return content.toMap();
  }

  public String getSerializedContent() {
    return content.toString();
  }

  public byte[] getSerializedContentAsBytes() {
    return content.payload;
  }

  public String getGroupKeyId() {
    return groupKeyId;
  }

  public EncryptedGroupKey getNewGroupKey() {
    return newGroupKey;
  }

  public String serialize() {
    return StreamMessageAdapter.serialize(this);
  }

  public static StreamMessage deserialize(String json) {
    return StreamMessageAdapter.deserialize(json);
  }

  public byte[] toBytes() {
    return serialize().getBytes(StandardCharsets.UTF_8);
  }

  public int sizeInBytes() {
    return toBytes().length;
  }

  public static StreamMessage fromBytes(byte[] bytes) throws IOException {
    return StreamMessage.deserialize(new String(bytes, StandardCharsets.UTF_8));
  }

  @Override
  public String toString() {
    return String.format(
        "%s{messageId=%s, previousMessageRef=%s, content=%s, encryptionType=%s, groupKeyId='%s', newGroupKey='%s', signatureType=%s, signature='%s'}",
        messageType,
        messageId,
        previousMessageRef,
        content,
        encryptionType,
        groupKeyId,
        newGroupKey,
        signatureType,
        signature);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final StreamMessage that = (StreamMessage) o;
    return Objects.equals(serialize(), that.serialize());
  }

  @Override
  public int hashCode() {
    return Objects.hash(serialize());
  }

  public static final class Builder {
    private MessageId messageId;
    private MessageRef previousMessageRef;
    private MessageType messageType = MessageType.STREAM_MESSAGE;
    private Content content;
    private EncryptionType encryptionType = EncryptionType.NONE;
    private String groupKeyId = null;
    private EncryptedGroupKey newGroupKey = null;
    private SignatureType signatureType = SignatureType.NONE;
    private String signature = null;

    public Builder() {}

    public Builder(final StreamMessage message) {
      Objects.requireNonNull(message);
      this.messageId = message.messageId;
      this.previousMessageRef = message.previousMessageRef;
      this.messageType = message.messageType;
      this.content = message.content;
      this.encryptionType = message.encryptionType;
      this.groupKeyId = message.groupKeyId;
      this.newGroupKey = message.newGroupKey;
      this.signatureType = message.signatureType;
      this.signature = message.signature;
    }

    public Builder withMessageId(final MessageId messageId) {
      this.messageId = messageId;
      return this;
    }

    public Builder withPreviousMessageRef(final MessageRef previousMessageRef) {
      this.previousMessageRef = previousMessageRef;
      return this;
    }

    public Builder withMessageType(final MessageType messageType) {
      this.messageType = messageType;
      return this;
    }

    public Builder withContent(final Content content) {
      Objects.requireNonNull(content);
      this.content = content;
      return this;
    }

    public Builder withEncryptionType(final EncryptionType encryptionType) {
      this.encryptionType = encryptionType;
      return this;
    }

    public Builder withGroupKeyId(final String groupKeyId) {
      this.groupKeyId = groupKeyId;
      return this;
    }

    public Builder withNewGroupKey(final EncryptedGroupKey newGroupKey) {
      if ((newGroupKey != null) && newGroupKey.getGroupKeyId().equals(groupKeyId)) {
        final String msg =
            String.format(
                "newGroupKey isn't new - it matches the groupKeyId of the message: %s",
                newGroupKey.getGroupKeyId());
        throw new IllegalArgumentException(msg);
      }
      this.newGroupKey = newGroupKey;
      return this;
    }

    public Builder withSignatureType(final SignatureType signatureType) {
      this.signatureType = signatureType;
      return this;
    }

    public Builder withSignature(final String signature) {
      this.signature = signature;
      return this;
    }

    public StreamMessage createStreamMessage() {
      return new StreamMessage(
          messageId,
          previousMessageRef,
          messageType,
          content,
          encryptionType,
          groupKeyId,
          newGroupKey,
          signatureType,
          signature);
    }
  }
}
