package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger log = LogManager.getLogger();
    private static final Map<Integer, JsonAdapter<StreamMessage>> adapterByVersion = new HashMap<>();
    private static final StreamMessageAdapter staticAdapter = new StreamMessageAdapter();

    static {
        adapterByVersion.put(30, new StreamMessageV30Adapter());
        adapterByVersion.put(31, new StreamMessageV31Adapter());
    }

    /**
     * Serializes the message to the latest version
     */
    public static String serialize(StreamMessage msg) {
        return staticAdapter.toJson(msg);
    }

    public static StreamMessage deserialize(String json) throws MalformedMessageException {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, StandardCharsets.UTF_8));
        try {
            return staticAdapter.fromJson(reader);
        } catch (Exception e) {
            log.error(e);
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
        writer.beginArray();
        adapterByVersion.get(StreamMessage.LATEST_VERSION).toJson(writer, value);
        writer.endArray();
    }

}
