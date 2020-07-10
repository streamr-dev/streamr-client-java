package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StreamMessageV30Adapter extends JsonAdapter<StreamMessageV30> {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageV30Adapter.class);
    private static final MessageIDAdapter msgIdAdapter = new MessageIDAdapter();
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    @Override
    public StreamMessageV30 fromJson(JsonReader reader) throws IOException {
        try {
            MessageID messageID = msgIdAdapter.fromJson(reader);
            MessageRef previousMessageRef = null;
            // Peek at the previousMessageRef, as it can be null
            if (reader.peek().equals(JsonReader.Token.NULL)) {
                reader.nextNull();
            } else {
                previousMessageRef = msgRefAdapter.fromJson(reader);
            }
            ContentType contentType = ContentType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String signature = null;
            if (signatureType != SignatureType.SIGNATURE_TYPE_NONE) {
                signature = reader.nextString();
            } else {
                reader.nextNull();
            }

            return new StreamMessageV30(messageID, previousMessageRef, contentType, serializedContent, signatureType, signature);
        } catch (JsonDataException e) {
            log.error("Failed to parse StreamMessageV30", e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessageV30 value) throws IOException {
        msgIdAdapter.toJson(writer, value.getMessageID());
        if (value.getPreviousMessageRef() != null) {
            msgRefAdapter.toJson(writer, value.getPreviousMessageRef());
        }else {
            writer.value((String)null);
        }
        writer.value(value.getContentType().getId());
        writer.value(value.getSerializedContent());
        writer.value(value.getSignatureType().getId());
        writer.value(value.getSignature());
    }
}
