package com.streamr.client.protocol;

import com.squareup.moshi.*;
import com.streamr.client.BroadcastMessage;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;

import java.io.IOException;
import java.util.Map;

public class StreamMessageAdapter extends JsonAdapter<BroadcastMessage> {

    // Thread safe
    private static final Moshi moshi = new Moshi.Builder().build();
    private static final JsonAdapter<Map> mapAdapter = moshi.adapter(Map.class);

    @Override
    public BroadcastMessage fromJson(JsonReader reader) throws IOException {
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
            long previousOffset = reader.nextLong();
            int contentType = reader.nextInt();

            // Parse payload as JSON
            Object payload;
            if (contentType == 27) {
                payload = mapAdapter.fromJson(reader.nextString());
            } else {
                throw new UnsupportedMessageException("Unrecognized payload type: " + contentType);
            }

            reader.endArray();

            return new BroadcastMessage(streamId, partition, timestamp, ttl, offset, previousOffset, contentType, payload);
        } catch (JsonDataException e) {
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, BroadcastMessage value) throws IOException {
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
