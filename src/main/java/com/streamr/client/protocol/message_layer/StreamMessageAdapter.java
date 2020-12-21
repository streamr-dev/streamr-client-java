package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A JsonAdapter that is able to:
 * - read all versions of Stream Layer protocol
 * - write the latest version of Stream Layer protocol
 */
public class StreamMessageAdapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageAdapter.class);

    private static final Map<Integer, JsonAdapter<StreamMessage>> adapterByVersion = new HashMap<>();
    private static final StreamMessageAdapter staticAdapter = new StreamMessageAdapter();

    static {
        adapterByVersion.put(30, new StreamMessageV30Adapter());
        adapterByVersion.put(31, new StreamMessageV31Adapter());
        adapterByVersion.put(32, new StreamMessageV32Adapter());
    }

    /**
     * Serializes the message to the latest version
     */
    public static String serialize(StreamMessage msg) {
        return staticAdapter.toJson(msg);
    }

    public static String serialize(StreamMessage msg, int version) {
        JsonAdapter<StreamMessage> adapter = adapterByVersion.get(version);
        if (adapter == null) {
            throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
        }
        return adapter.toJson(msg);
    }

    public static StreamMessage deserialize(String json) throws MalformedMessageException {
        final JsonReader reader;
        try (final Buffer buffer = new Buffer()) {
            reader = JsonReader.of(buffer.writeString(json, StandardCharsets.UTF_8));
        }
        try {
            return staticAdapter.fromJson(reader);
        } catch (Exception e) {
            log.error("Failed to parse StreamMessage", e);
            throw new MalformedMessageException("Unable to deserialize message: " + json, e);
        }
    }

    /**
     * Used when serializing and deserializing Control Layer messages with inline StreamMessages
     */
    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException, MalformedMessageException {
        // Read version, then delegate to correct adapter
        reader.beginArray();
        int version = reader.nextInt();

        JsonAdapter<StreamMessage> adapter = adapterByVersion.get(version);
        if (adapter == null) {
            throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
        }

        StreamMessage msg = adapter.fromJson(reader);
        reader.endArray();
        return msg;
    }

    /**
     * Used when serializing and deserializing Control Layer messages with inline StreamMessages.
     * Note: serializes to StreamMessage.LATEST_VERSION.
     */
    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        adapterByVersion.get(StreamMessage.LATEST_VERSION).toJson(writer, value);
    }

}
