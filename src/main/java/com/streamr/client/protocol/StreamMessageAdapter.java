package com.streamr.client.protocol;

import com.squareup.moshi.*;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class StreamMessageAdapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LogManager.getLogger();

    // Thread safe
    private static final Moshi moshi = new Moshi.Builder().build();
    private static final JsonAdapter<Map> mapAdapter = moshi.adapter(Map.class);

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();
            if (version != 28) {
                throw new UnsupportedMessageException("Unrecognized broadcast message version: " + version);
            }

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

            int contentType = reader.nextInt();

            // Parse payload as JSON
            Object payload;
            if (contentType == 27) {
                payload = mapAdapter.fromJson(reader.nextString());
            } else {
                throw new UnsupportedMessageException("Unrecognized payload type: " + contentType);
            }

            reader.endArray();

            return new StreamMessage(streamId, partition, timestamp, ttl, offset, previousOffset, contentType, payload);
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        // TODO
        /*
        writer.beginArray();
        writer.value(0); // version
        writer.value(value.getMessageTypeCode()); // type
        writer.value(value.getSubscriptionId()); // subscription id
        writer.value(adapterByCode[value.getMessageTypeCode()].toJson(value.getPayload()));
        writer.endArray();
        */
    }
}
