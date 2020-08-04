package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import com.streamr.client.utils.EncryptedGroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

public class StreamMessageV32Adapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageV32Adapter.class);

    private static final int VERSION = 32;
    private static final MessageIDAdapter msgIdAdapter = new MessageIDAdapter();
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    private static <T> T nullSafeRead(JsonReader reader, Callable<T> unsafeGetter) throws Exception {
        if (reader.peek().equals(JsonReader.Token.NULL)) {
            return reader.nextNull();
        } else {
            return unsafeGetter.call();
        }
    }

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            // top-level array has already been opened in StreamMessageAdapter
            // version field has already been read in StreamMessageAdapter
            MessageID messageID = msgIdAdapter.fromJson(reader);
            MessageRef previousMessageRef = nullSafeRead(reader, () -> msgRefAdapter.fromJson(reader));
            StreamMessage.MessageType messageType = StreamMessage.MessageType.fromId((byte)reader.nextInt());
            StreamMessage.ContentType contentType = StreamMessage.ContentType.fromId((byte)reader.nextInt());
            EncryptionType encryptionType = EncryptionType.fromId((byte)reader.nextInt());
            String groupKeyId = nullSafeRead(reader, reader::nextString);
            String serializedContent = reader.nextString();
            String serializedNewGroupKey = nullSafeRead(reader, reader::nextString);
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String signature = nullSafeRead(reader, reader::nextString);
            // top-level array will be closed in StreamMessageAdapter

            return new StreamMessage(
                    messageID,
                    previousMessageRef,
                    messageType,
                    serializedContent,
                    contentType,
                    encryptionType,
                    groupKeyId,
                    serializedNewGroupKey != null ? EncryptedGroupKey.deserialize(serializedNewGroupKey) : null,
                    signatureType,
                    signature
            );
        } catch (Exception e) {
            log.error("Failed to parse StreamMessageV31", e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        writer.beginArray();
        writer.value(VERSION);
        msgIdAdapter.toJson(writer, value.getMessageID());

        if (value.getPreviousMessageRef() != null) {
            msgRefAdapter.toJson(writer, value.getPreviousMessageRef());
        } else {
            writer.value((String) null);
        }

        writer.value(value.getMessageType().getId());
        writer.value(value.getContentType().getId());
        writer.value(value.getEncryptionType().getId());
        writer.value(value.getGroupKeyId());
        writer.value(value.getSerializedContent());

        if (value.getNewGroupKey() != null) {
            writer.value(value.getNewGroupKey().serialize());
        } else {
            writer.value((String) null);
        }

        writer.value(value.getSignatureType().getId());
        writer.value(value.getSignature());
        writer.endArray();
    }
}
