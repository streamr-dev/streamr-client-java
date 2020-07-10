package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StreamMessageAdapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LoggerFactory.getLogger(StreamMessageAdapter.class);
    private static final StreamMessageV30Adapter v30Adapter = new StreamMessageV30Adapter();
    private static final StreamMessageV31Adapter v31Adapter = new StreamMessageV31Adapter();

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();
            StreamMessage msg;
            if (version == StreamMessageV30.VERSION) {
                msg = v30Adapter.fromJson(reader);
            } else if (version == StreamMessageV31.VERSION) {
                msg = v31Adapter.fromJson(reader);
            } else {
                throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
            }
            reader.endArray();
            return msg;
        } catch (JsonDataException e) {
            log.error("Failed to parse StreamMessage", e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        writer.beginArray();
        int version = value.getVersion();
        writer.value(version);
        if (version == StreamMessageV30.VERSION) {
            v30Adapter.toJson(writer, (StreamMessageV30) value);
        } else if (version == StreamMessageV31.VERSION) {
            v31Adapter.toJson(writer, (StreamMessageV31) value);
        } else {
            throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
        }
        writer.endArray();
    }
}
