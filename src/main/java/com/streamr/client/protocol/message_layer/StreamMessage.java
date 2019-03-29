package com.streamr.client.protocol.message_layer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamMessage implements ITimestamped {

    private static final Logger log = LogManager.getLogger();
    private static final StreamMessageAdapter adapter = new StreamMessageAdapter();

    public enum ContentType {
        CONTENT_TYPE_JSON ((byte) 27);

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

    private int version;
    protected ContentType contentType;
    // Payload type might need to be changed to Object when new
    // non-JSON payload types are introduced
    protected Map<String, Object> content;
    protected String serializedContent;

    public StreamMessage(int version, ContentType contentType, String serializedContent) {
        this.version = version;
        this.contentType = contentType;
        this.serializedContent = serializedContent;
    }

    public StreamMessage(int version, ContentType contentType, Map<String, Object> content){
        this.version = version;
        this.contentType = contentType;
        if (contentType == ContentType.CONTENT_TYPE_JSON) {
            this.content = content;
            this.serializedContent = HttpUtils.mapAdapter.toJson(content);
        } else {
            throw new UnsupportedMessageException("Unrecognized payload type: " + contentType);
        }
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

    public ContentType getContentType() {
        return contentType;
    }

    public Map<String, Object> getContent() throws IOException {
        if (content == null) {
            if (contentType == ContentType.CONTENT_TYPE_JSON) {
                if(serializedContent.equals("")) {
                    this.content = new HashMap<String, Object>();
                } else {
                    this.content = HttpUtils.mapAdapter.fromJson(serializedContent);
                }
            } else {
                throw new UnsupportedMessageException("Unrecognized payload type: " + contentType);
            }
        }
        return content;
    }

    public String getSerializedContent() {
        return serializedContent;
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
}
