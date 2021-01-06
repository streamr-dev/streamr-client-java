package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageAdapter;

import java.io.IOException;

public class UnicastMessageAdapter extends ControlLayerAdapter<UnicastMessage> {

    private static final StreamMessageAdapter streamMessageAdapter = new StreamMessageAdapter();

    UnicastMessageAdapter() {
        super(UnicastMessage.class);
    }

    @Override
    public UnicastMessage fromJson(JsonReader reader) throws IOException {
        // Version and type already read
        String requestId = reader.nextString();
        StreamMessage streamMessage = streamMessageAdapter.fromJson(reader);
        return new UnicastMessage(requestId, streamMessage);
    }

    @Override
    public void toJson(JsonWriter writer, UnicastMessage value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(UnicastMessage.TYPE);
        writer.value(value.getRequestId());
        streamMessageAdapter.toJson(writer, value.getStreamMessage());
        writer.endArray();
    }
}
