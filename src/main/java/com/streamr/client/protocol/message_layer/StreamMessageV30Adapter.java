package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class StreamMessageV30Adapter extends JsonAdapter<StreamMessageV30> {

    private static final Logger log = LogManager.getLogger();
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    @Override
    public StreamMessageV30 fromJson(JsonReader reader) throws IOException {
        try {
            MessageID messageID = messageIDFromJson(reader);
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
            if (signatureType == SignatureType.SIGNATURE_TYPE_ETH) {
                signature = reader.nextString();
            } else {
                reader.nextNull();
            }

            return new StreamMessageV30(messageID, previousMessageRef, contentType, serializedContent, signatureType, signature);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    private MessageID messageIDFromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();
            String streamId = reader.nextString();
            int streamPartition = reader.nextInt();
            long timestamp = reader.nextLong();
            int sequenceNumber = reader.nextInt();
            String publisherId = reader.nextString();
            reader.endArray();

            return new MessageID(streamId, streamPartition, timestamp, sequenceNumber, publisherId);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message id: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessageV30 value) throws IOException {
        writer.beginArray();
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getTimestamp());
        writer.value(value.getSequenceNumber());
        writer.value(value.getPublisherId());
        writer.endArray();
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
