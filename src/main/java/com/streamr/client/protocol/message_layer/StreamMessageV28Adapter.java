package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class StreamMessageV28Adapter extends JsonAdapter<StreamMessageV28> {

    private static final Logger log = LogManager.getLogger();

    @Override
    public StreamMessageV28 fromJson(JsonReader reader) throws IOException {
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

            return new StreamMessageV28(streamId, partition, timestamp, ttl, offset, previousOffset, contentType, serializedContent);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessageV28 value) throws IOException {
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getTimestamp());
        writer.value(value.getTtl());
        writer.value(value.getOffset());
        writer.value(value.getPreviousOffset());
        writer.value(value.getContentType().getId());
        writer.value(value.getSerializedContent());
    }
}
