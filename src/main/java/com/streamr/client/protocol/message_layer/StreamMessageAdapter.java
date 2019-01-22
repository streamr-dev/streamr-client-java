package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.streamr.client.protocol.message_layer.StreamMessage.ContentType;
import com.streamr.client.protocol.message_layer.StreamMessage.SignatureType;

import java.io.IOException;

public class StreamMessageAdapter extends JsonAdapter<StreamMessage> {

    private static final Logger log = LogManager.getLogger();
    private static final StreamMessageV28Adapter v28Adapter = new StreamMessageV28Adapter();
    private static final StreamMessageV29Adapter v29Adapter = new StreamMessageV29Adapter();
    private static final StreamMessageV30Adapter v30Adapter = new StreamMessageV30Adapter();

    @Override
    public StreamMessage fromJson(JsonReader reader) throws IOException {
        try {
            reader.beginArray();

            // Check version
            int version = reader.nextInt();
            StreamMessage msg;
            if (version == StreamMessageV28.VERSION) {
                msg = v28Adapter.fromJson(reader);
            } else if (version == StreamMessageV29.VERSION) {
                msg = v29Adapter.fromJson(reader);
            } else if (version == StreamMessageV30.VERSION) {
                msg = v30Adapter.fromJson(reader);
            } else {
                throw new UnsupportedMessageException("Unrecognized stream message version: " + version);
            }
            reader.endArray();
            return msg;
        } catch (JsonDataException e) {
            log.error(e);
            throw new MalformedMessageException("Malformed message: " + reader.toString(), e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, StreamMessage value) throws IOException {
        writer.beginArray();
        writer.value(value.getVersion());
        value.writeJson(writer);
        writer.endArray();
    }
}
