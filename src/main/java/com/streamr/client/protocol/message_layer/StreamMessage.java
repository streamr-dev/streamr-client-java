package com.streamr.client.protocol.message_layer;

import com.streamr.client.exceptions.EncryptedContentNotParsableException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.utils.HttpUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class StreamMessage implements ITimestamped {

    private StreamMessageAdapter adapter = new StreamMessageAdapter();
    private static final Logger log = LogManager.getLogger();

    public static final int LATEST_VERSION = 31;

    public enum MessageType {
        STREAM_MESSAGE ((byte) 27),
        GROUP_KEY_REQUEST ((byte) 28),
        GROUP_KEY_RESPONSE_SIMPLE ((byte) 29),
        GROUP_KEY_RESET_SIMPLE ((byte) 30),
        GROUP_KEY_RESPONSE_ERROR((byte) 31),
        GROUP_KEY_ROTATE((byte) 32);

        private final byte id;

        MessageType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }

        public static MessageType fromId(byte id) {
            if (id == STREAM_MESSAGE.id) {
                return STREAM_MESSAGE;
            } else if (id == GROUP_KEY_REQUEST.id) {
                return GROUP_KEY_REQUEST;
            } else if (id == GROUP_KEY_RESPONSE_SIMPLE.id) {
                return GROUP_KEY_RESPONSE_SIMPLE;
            } else if (id == GROUP_KEY_RESET_SIMPLE.id) {
                return GROUP_KEY_RESET_SIMPLE;
            } else if (id == GROUP_KEY_RESPONSE_ERROR.id) {
                return GROUP_KEY_RESPONSE_ERROR;
            } else if (id == GROUP_KEY_ROTATE.id) {
                return GROUP_KEY_ROTATE;
            }
            throw new UnsupportedMessageException("Unrecognized content type: "+id);
        }
    }

    public enum ContentType {
        JSON ((byte) 0);

        private final byte id;

        ContentType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }

        public static ContentType fromId(byte id) {
            if (id == JSON.id) {
                return JSON;
            }
            throw new UnsupportedMessageException("Unrecognized ContentType: "+id);
        }
    }

    public enum SignatureType {
        SIGNATURE_TYPE_NONE ((byte) 0),
        SIGNATURE_TYPE_ETH_LEGACY ((byte) 1),
        SIGNATURE_TYPE_ETH ((byte) 2);

        private final byte id;

        SignatureType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }

        public static SignatureType fromId(byte id) {
            if (id == SIGNATURE_TYPE_NONE.id) {
                return SIGNATURE_TYPE_NONE;
            } else if (id == SIGNATURE_TYPE_ETH_LEGACY.id) {
                return SIGNATURE_TYPE_ETH_LEGACY;
            } else if (id == SIGNATURE_TYPE_ETH.id) {
                return SIGNATURE_TYPE_ETH;
            }
            throw new UnsupportedMessageException("Unrecognized signature type: "+id);
        }
    }

    public enum EncryptionType {
        NONE ((byte) 0),
        RSA ((byte) 1),
        AES ((byte) 2),
        NEW_KEY_AND_AES ((byte) 3);

        private final byte id;

        EncryptionType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }

        public static EncryptionType fromId(byte id) {
            if (id == NONE.id) {
                return NONE;
            } else if (id == RSA.id) {
                return RSA;
            } else if (id == AES.id) {
                return AES;
            } else if (id == NEW_KEY_AND_AES.id) {
                return NEW_KEY_AND_AES;
            }
            throw new UnsupportedMessageException("Unrecognized encryption type: "+id);
        }
    }
    private final MessageID messageID;
    private MessageRef previousMessageRef;
    private final MessageType messageType;
    private Map<String, Object> parsedContent; // Might need to change to Object when non-JSON contentTypes are introduced
    private String serializedContent;
    private final ContentType contentType;
    private EncryptionType encryptionType;
    private String groupKeyId;
    private SignatureType signatureType;
    private String signature;

    /**
     * Full constructor, creates a StreamMessage with all fields directly set to the provided values.
     */
    public StreamMessage(
            MessageID messageID,
            MessageRef previousMessageRef,
            MessageType messageType,
            String serializedContent,
            ContentType contentType,
            EncryptionType encryptionType,
            String groupKeyId,
            SignatureType signatureType,
            String signature
    ) {
        this.messageID = messageID;
        this.previousMessageRef = previousMessageRef;
        this.messageType = messageType;
        this.serializedContent = serializedContent;
        this.contentType = contentType;
        this.encryptionType = encryptionType;
        this.groupKeyId = groupKeyId;
        this.signatureType = signatureType;
        this.signature = signature;
    }

    /**
     * Convenience constructor. Serializes the provided Map to JSON and sets ContentType to JSON.
     */
    public StreamMessage(
            MessageID messageID,
            MessageRef previousMessageRef,
            MessageType messageType,
            Map<String, Object> content,
            EncryptionType encryptionType,
            String groupKeyId,
            SignatureType signatureType,
            String signature
    ) {
        this(messageID, previousMessageRef, messageType, HttpUtils.mapAdapter.toJson(content), ContentType.JSON, encryptionType, groupKeyId, signatureType, signature);
    }

    /**
     * Convenience constructor. Serializes the provided Map to JSON and sets ContentType to JSON.
     * Leaves the encryption and signature related fields at their default (empty/inactive) values.
     */
    public StreamMessage(
            MessageID messageID,
            MessageRef previousMessageRef,
            MessageType messageType,
            Map<String, Object> content
    ) {
        this(messageID, previousMessageRef, messageType, HttpUtils.mapAdapter.toJson(content), ContentType.JSON, EncryptionType.NONE, null, SignatureType.SIGNATURE_TYPE_NONE, null);
    }

    /**
     * Convenience constructor. Serializes the provided Map to JSON, sets ContentType to JSON, and MessageType to STREAM_MESSAGE.
     * Leaves the encryption and signature related fields at their default (empty/inactive) values.
     */
    public StreamMessage(
            MessageID messageID,
            MessageRef previousMessageRef,
            Map<String, Object> content
    ) {
        this(messageID, previousMessageRef, MessageType.STREAM_MESSAGE, HttpUtils.mapAdapter.toJson(content), ContentType.JSON, EncryptionType.NONE, null, SignatureType.SIGNATURE_TYPE_NONE, null);
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

    public String getPublisherId() {
        return messageID.getPublisherId();
    }

    public String getMsgChainId() {
        return messageID.getMsgChainId();
    }

    public MessageRef getPreviousMessageRef() {
        return previousMessageRef;
    }

    public void setPreviousMessageRef(MessageRef previousMessageRef) {
        this.previousMessageRef = previousMessageRef;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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
                    this.parsedContent = HttpUtils.mapAdapter.fromJson(serializedContent);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to parse message content: " + serializedContent);
                }
            } else {
                throw new RuntimeException("Unknown contentType encountered: " + contentType);
            }
            validateContent(parsedContent, messageType);
        }
        return parsedContent;
    }

    public String getSerializedContent() {
        return serializedContent;
    }

    public byte[] getSerializedContentAsBytes() {
        return serializedContent.getBytes(StandardCharsets.UTF_8);
    }

    public void setEncryptionType(EncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    public String getGroupKeyId() {
        return groupKeyId;
    }

    public void setGroupKeyId(String groupKeyId) {
        this.groupKeyId = groupKeyId;
    }

    public void setSerializedContent(String serializedContent) throws IOException {
        if (this.encryptionType == EncryptionType.NONE) {
            this.parsedContent = HttpUtils.mapAdapter.fromJson(serializedContent);
            validateContent(parsedContent, messageType);
        } else {
            this.parsedContent = null;
        }
        this.serializedContent = serializedContent;
    }

    public void setSerializedContent(byte[] serializedContent) throws IOException {
        setSerializedContent(new String(serializedContent, StandardCharsets.UTF_8));
    }

    public void setParsedContent(Map<String, Object> parsedContent) {
        validateContent(parsedContent, messageType);
        this.parsedContent = parsedContent;
        this.serializedContent = HttpUtils.mapAdapter.toJson(parsedContent);
    }

    public String serialize() {
        return serialize(LATEST_VERSION);
    }

    public String serialize(int version) {
        return adapter.serialize(this, version);
    }

    public static StreamMessage deserialize(String json) throws IOException {
        return StreamMessageAdapter.deserialize(json);
    }

    public byte[] toBytes() {
        return serialize().getBytes(StandardCharsets.UTF_8);
    }

    public int sizeInBytes(){
        return toBytes().length;
    }

    public static StreamMessage fromBytes(byte[] bytes) throws IOException {
        return StreamMessage.deserialize(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void validateContent(Map<String, Object> content, MessageType messageType) {
        if (messageType != MessageType.STREAM_MESSAGE) {
            // Throws if the content is not valid
            AbstractGroupKeyMessage.fromContent(content, messageType);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.serialize();
    }
}
