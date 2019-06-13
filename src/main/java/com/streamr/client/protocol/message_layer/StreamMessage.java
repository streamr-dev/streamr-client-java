package com.streamr.client.protocol.message_layer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.EncryptedContentNotParsableException;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamMessage implements ITimestamped {

    private static final Logger log = LogManager.getLogger();
    private static final StreamMessageAdapter adapter = new StreamMessageAdapter();

    public enum ContentType {
        CONTENT_TYPE_JSON ((byte) 27),
        GROUP_KEY_REQUEST ((byte) 28),
        GROUP_KEY_RESPONSE_SIMPLE ((byte) 29),
        GROUP_KEY_RESET_SIMPLE ((byte) 30);

        private final byte id;

        ContentType(byte id) {
            this.id = id;
        }

        public byte getId() {
            return this.id;
        }

        public static ContentType fromId(byte id) {
            if (id == CONTENT_TYPE_JSON.id) {
                return CONTENT_TYPE_JSON;
            } else if (id == GROUP_KEY_REQUEST.id) {
                return GROUP_KEY_REQUEST;
            } else if (id == GROUP_KEY_RESPONSE_SIMPLE.id) {
                return GROUP_KEY_RESPONSE_SIMPLE;
            } else if (id == GROUP_KEY_RESET_SIMPLE.id) {
                return GROUP_KEY_RESET_SIMPLE;
            }
            throw new UnsupportedMessageException("Unrecognized content type: "+id);
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

    private int version;
    protected ContentType contentType;
    protected EncryptionType encryptionType;
    // Payload type might need to be changed to Object when new
    // non-JSON payload types are introduced
    protected Map<String, Object> content;
    protected String serializedContent;

    public StreamMessage(int version, ContentType contentType, EncryptionType encryptionType, String serializedContent) {
        this.version = version;
        this.contentType = contentType;
        this.encryptionType = encryptionType;
        this.serializedContent = serializedContent;
    }

    public StreamMessage(int version, ContentType contentType, EncryptionType encryptionType, Map<String, Object> content){
        this.version = version;
        this.contentType = contentType;
        this.encryptionType = encryptionType;
        this.content = content;
        validateContent(content, contentType);
        this.serializedContent = HttpUtils.mapAdapter.toJson(content);
    }

    public int getVersion() {
        return version;
    }

    public abstract String getStreamId();

    public abstract int getStreamPartition();

    public abstract long getTimestamp();

    @Override
    public Date getTimestampAsDate() {
        return new Date(getTimestamp());
    }

    public abstract long getSequenceNumber();

    public abstract String getPublisherId();

    public abstract String getMsgChainId();

    public abstract MessageRef getPreviousMessageRef();

    public MessageRef getMessageRef() {
        return new MessageRef(getTimestamp(), getSequenceNumber());
    }

    public ContentType getContentType() {
        return contentType;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public Map<String, Object> getContent() throws IOException, EncryptedContentNotParsableException {
        if (content == null) {
            if (encryptionType != EncryptionType.NONE) {
                throw new EncryptedContentNotParsableException(encryptionType);
            }
            this.content = HttpUtils.mapAdapter.fromJson(serializedContent);
            validateContent(content, contentType);
        }
        return content;
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

    public void setSerializedContent(String serializedContent) throws IOException {
        this.serializedContent = serializedContent;
        if (this.encryptionType == EncryptionType.NONE) {
            this.content = HttpUtils.mapAdapter.fromJson(serializedContent);
            validateContent(content, contentType);
        }
    }

    public void setSerializedContent(byte[] serializedContent) throws IOException {
        setSerializedContent(new String(serializedContent, StandardCharsets.UTF_8));
    }

    public void setContent(Map<String, Object> content) {
        validateContent(content, contentType);
        this.content = content;
        this.serializedContent = HttpUtils.mapAdapter.toJson(content);
    }

    public abstract SignatureType getSignatureType();

    public abstract String getSignature();

    public abstract void setSignatureType(SignatureType signatureType);

    public abstract void setSignature(String signature);

    public String toJson(){
        try {
            Buffer buffer = new Buffer();
            JsonWriter writer = JsonWriter.of(buffer);
            adapter.toJson(writer, this);
            return buffer.readUtf8();
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    public byte[] toBytes() {
        return toJson().getBytes(StandardCharsets.UTF_8);
    }

    public int sizeInBytes(){
        return toBytes().length;
    }

    private static JsonReader toReader(String json) {
        return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")));
    }

    public static StreamMessage fromJson(String json) throws IOException {
        return adapter.fromJson(toReader(json));
    }

    public static StreamMessage fromBytes(byte[] bytes) throws IOException {
        return StreamMessage.fromJson(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void validateContent(Map<String, Object> content, ContentType contentType) {
        if (contentType == ContentType.GROUP_KEY_REQUEST) {
            if (!content.containsKey("publicKey")) {
                throw new MalformedMessageException("Content of type " + ContentType.GROUP_KEY_REQUEST + " must contain a 'publicKey' field.");
            }
            if (content.containsKey("range")) {
                try {
                    Map<String, Object> range = (Map<String, Object>) content.get("range");
                    if (!range.containsKey("start") || !range.containsKey("end")) {
                        throw new MalformedMessageException("Content of type " + ContentType.GROUP_KEY_REQUEST + " must contain 'start' and 'end' fields.");
                    }
                } catch (ClassCastException e) {
                    throw new MalformedMessageException("'range' field must be a Map<String,Object>.");
                }

            }
        } else if (contentType == ContentType.GROUP_KEY_RESPONSE_SIMPLE) {
            if (!content.containsKey("keys")) {
                throw new MalformedMessageException("Content of type " + ContentType.GROUP_KEY_RESPONSE_SIMPLE + " must contain a 'keys' field.");
            }
            try {
                List<Map<String,Object>> keys = (List<Map<String,Object>>) content.get("keys");
                for (Map<String, Object> key: keys) {
                    if (!key.containsKey("groupKey") || !key.containsKey("start")) {
                        throw new MalformedMessageException("Each element in field 'keys' of content of type " +
                                ContentType.GROUP_KEY_RESPONSE_SIMPLE + " must contain 'groupKey' and 'start' fields.");
                    }
                }
            } catch (ClassCastException e) {
                throw new MalformedMessageException("'keys' field must be a List<Map<String,Object>>.");
            }

        } else if (contentType == ContentType.GROUP_KEY_RESET_SIMPLE) {
            if (!content.containsKey("groupKey") || !content.containsKey("start")) {
                throw new MalformedMessageException("Content of type " + ContentType.GROUP_KEY_RESET_SIMPLE + " must contain 'groupKey' and 'start' fields.");
            }
        }
    }
}
