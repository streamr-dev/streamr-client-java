package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageAdapter;

import java.io.IOException;

public class PublishRequestAdapter extends ControlLayerAdapter<PublishRequest> {
    private static final StreamMessageAdapter streamMessageAdapter = new StreamMessageAdapter();

    PublishRequestAdapter() {
        super(PublishRequest.class);
    }

    @Override
    public PublishRequest fromJson(JsonReader reader) throws IOException {
        // Version and type already read
        String requestId = reader.nextString();
        StreamMessage streamMessage = streamMessageAdapter.fromJson(reader);
        String sessionToken = nullSafe(reader, r ->r.nextString());
        return new PublishRequest(requestId, streamMessage, sessionToken);
    }

    @Override
    public void toJson(JsonWriter writer, PublishRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(PublishRequest.TYPE);
        writer.value(value.getRequestId());
        streamMessageAdapter.toJson(writer, value.getStreamMessage());
        writer.value(value.getSessionToken());
        writer.endArray();
    }
}
