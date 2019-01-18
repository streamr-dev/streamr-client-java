package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;

import java.io.IOException;

public class StreamMessageAdapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LogManager.getLogger();

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();
            if (version == StreamMessageV28.VERSION) {
                return fromJsonV28(reader);
            } else if (version == StreamMessageV29.VERSION) {
                return fromJsonV29(reader);
            } else if (version == StreamMessageV30.VERSION) {
                return fromJsonV30(reader);
            } else {
                throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
            }
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    private StreamMessageV28 fromJsonV28(JsonReader reader) throws IOException {
        try {
            String streamId = reader.nextString();
            int partition = reader.nextInt();
            long timestamp = reader.nextLong();
            int ttl = reader.nextInt();
            long offset = reader.nextLong();
            Long previousOffset = null;
            // Peek at the previousOffset, as it can be null
            if (reader.peek().equals(JsonReader.Token.NULL)) {
                reader.nextNull();
            } else {
                previousOffset = reader.nextLong();
            }
            ContentType contentType = ContentType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            reader.endArray();

            return new StreamMessageV28(streamId, partition, timestamp, ttl, offset, previousOffset, contentType, serializedContent);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    private StreamMessageV29 fromJsonV29(JsonReader reader) throws IOException {
        try {
            String streamId = reader.nextString();
            int partition = reader.nextInt();
            long timestamp = reader.nextLong();
            int ttl = reader.nextInt();
            long offset = reader.nextLong();
            Long previousOffset = null;
            // Peek at the previousOffset, as it can be null
            if (reader.peek().equals(JsonReader.Token.NULL)) {
                reader.nextNull();
            } else {
                previousOffset = reader.nextLong();
            }
            ContentType contentType = ContentType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String publisherAddress = null;
            String signature = null;
            if (signatureType == SignatureType.SIGNATURE_TYPE_ETH) {
                publisherAddress = reader.nextString();
                signature = reader.nextString();
            }
            reader.endArray();

            return new StreamMessageV29(streamId, partition, timestamp, ttl, offset, previousOffset, contentType,
                    serializedContent, signatureType, publisherAddress, signature);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    private StreamMessageV30 fromJsonV30(JsonReader reader) throws IOException {
        try {
            MessageID messageID = messageIDFromJson(reader);
            MessageRef previousMessageRef = messageRefFromJson(reader);
            ContentType contentType = ContentType.fromId((byte)reader.nextInt());
            String serializedContent = reader.nextString();
            SignatureType signatureType = SignatureType.fromId((byte)reader.nextInt());
            String signature = null;
            if (signatureType == SignatureType.SIGNATURE_TYPE_ETH) {
                signature = reader.nextString();
            }
            reader.endArray();

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

    private MessageRef messageRefFromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();
            Long timestamp = null;
            // Peek at the previousTimestamp, as it can be null
            if (reader.peek().equals(JsonReader.Token.NULL)) {
                reader.nextNull();
            } else {
                timestamp = reader.nextLong();
            }
            int sequenceNumber = reader.nextInt();
            reader.endArray();

            return new MessageRef(timestamp, sequenceNumber);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message ref: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        writer.value(value.toJson());
    }
}
