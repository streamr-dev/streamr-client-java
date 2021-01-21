package com.streamr.client.protocol.message_layer;

import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.EncryptedGroupKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public final class StreamMessage implements ITimestamped {
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

  public enum ContentType {
    JSON((byte) 0);

    private final byte id;

    ContentType(final byte id) {
      this.id = id;
    }

    public byte getId() {
      return this.id;
    }

    public static ContentType fromId(final byte id) {
      if (id == JSON.id) {
        return JSON;
      }
      throw new UnsupportedMessageException("Unrecognized ContentType: " + id);
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

  private final MessageID messageID;
  private final MessageRef previousMessageRef;
  private final MessageType messageType;
  // Might need to change parsedContent to Object when non-JSON contentTypes are introduced
  private Map<String, Object> parsedContent;
  private final String serializedContent;
  private final ContentType contentType;
  private final EncryptionType encryptionType;
  private final String groupKeyId;
  private final EncryptedGroupKey newGroupKey;
  private final SignatureType signatureType;
  private final String signature;

  /**
   * Full constructor, creates a StreamMessage with all fields directly set to the provided values.
   */
  private StreamMessage(
      final MessageID messageID,
      final MessageRef previousMessageRef,
      final MessageType messageType,
      final String serializedContent,
      final ContentType contentType,
      final EncryptionType encryptionType,
      final String groupKeyId,
      final EncryptedGroupKey newGroupKey,
      final SignatureType signatureType,
      final String signature) {
    this.messageID = messageID;
    this.previousMessageRef = previousMessageRef;
    this.messageType = messageType;
    this.serializedContent = serializedContent;
    this.contentType = contentType;
    this.encryptionType = encryptionType;
    this.groupKeyId = groupKeyId;
    this.newGroupKey = newGroupKey;
    this.signatureType = signatureType;
    this.signature = signature;
  }

  public MessageID getMessageID() {
    return messageID;
  }

  public String getStreamId() {
    return messageID.getStreamId();
  }

  public int getStreamPartition() {
    return messageID.getStreamPartition();
  }

  public long getTimestamp() {
    return messageID.getTimestamp();
  }

  public long getSequenceNumber() {
    return messageID.getSequenceNumber();
  }

  public Address getPublisherId() {
    return messageID.getPublisherId();
  }

  public String getMsgChainId() {
    return messageID.getMsgChainId();
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

  @Override
  public Date getTimestampAsDate() {
    return new Date(getTimestamp());
  }

  public MessageRef getMessageRef() {
    return new MessageRef(getTimestamp(), getSequenceNumber());
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public ContentType getContentType() {
    return contentType;
  }

  public EncryptionType getEncryptionType() {
    return encryptionType;
  }

  public Map<String, Object> getParsedContent() {
    if (parsedContent == null) {
      if (encryptionType != EncryptionType.NONE) {
        throw new EncryptedContentNotParsableException(encryptionType);
      }
      if (contentType == ContentType.JSON) {
        try {
          this.parsedContent = Json.mapAdapter.fromJson(serializedContent);
        } catch (IOException e) {
          throw new RuntimeException("Failed to parse message content: " + serializedContent);
        }
      } else {
        throw new RuntimeException("Unknown contentType encountered: " + contentType);
      }
    }
    return parsedContent;
  }

  public String getSerializedContent() {
    return serializedContent;
  }

  public byte[] getSerializedContentAsBytes() {
    return serializedContent.getBytes(StandardCharsets.UTF_8);
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
        "%s{messageId=%s, previousMessageRef=%s, content='%s', contentType=%s, encryptionType=%s, groupKeyId='%s', newGroupKey='%s', signatureType=%s, signature='%s'}",
        messageType,
        messageID,
        previousMessageRef,
        serializedContent,
        contentType,
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
    private MessageID messageId;
    private MessageRef previousMessageRef;
    private MessageType messageType = MessageType.STREAM_MESSAGE;
    private String serializedContent;
    private ContentType contentType = ContentType.JSON;
    private EncryptionType encryptionType = EncryptionType.NONE;
    private String groupKeyId = null;
    private EncryptedGroupKey newGroupKey = null;
    private SignatureType signatureType = SignatureType.NONE;
    private String signature = null;

    public Builder() {}

    public Builder(final StreamMessage message) {
      this.messageId = message.messageID;
      this.previousMessageRef = message.previousMessageRef;
      this.messageType = message.messageType;
      this.serializedContent = message.serializedContent;
      this.contentType = message.contentType;
      this.encryptionType = message.encryptionType;
      this.groupKeyId = message.groupKeyId;
      this.newGroupKey = message.newGroupKey;
      this.signatureType = message.signatureType;
      this.signature = message.signature;
    }

    public Builder withMessageId(final MessageID messageId) {
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

    public Builder withSerializedContent(final String serializedContent) {
      this.serializedContent = serializedContent;
      return this;
    }

    public Builder withSerializedContent(final byte[] serializedContent) {
      this.serializedContent = new String(serializedContent, StandardCharsets.UTF_8);
      return this;
    }

    public Builder withContentType(final ContentType contentType) {
      this.contentType = contentType;
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
          serializedContent,
          contentType,
          encryptionType,
          groupKeyId,
          newGroupKey,
          signatureType,
          signature);
    }
  }
}
