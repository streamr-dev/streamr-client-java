package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StreamMessageAdapter {

    private static final Logger log = LogManager.getLogger();
    private static final Map<Integer, JsonAdapter<StreamMessage>> adapterByVersion = new HashMap<>();

    static {
        adapterByVersion.put(30, new StreamMessageV30Adapter());
        adapterByVersion.put(31, new StreamMessageV31Adapter());
    }

    public String serialize(StreamMessage msg, int version) {
        JsonAdapter<StreamMessage> adapter = adapterByVersion.get(version);
        if (adapter == null) {
            throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
        }

        return adapter.toJson(msg);
    }

    public static StreamMessage deserialize(String json) {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, StandardCharsets.UTF_8));

        // Read version, then delegate to correct adapter
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();

            JsonAdapter<StreamMessage> adapter = adapterByVersion.get(version);
            if (adapter == null) {
                throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
            }

            StreamMessage msg = adapter.fromJson(reader);
            reader.endArray();
            return msg;
        } catch (JsonDataException | IOException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

}
