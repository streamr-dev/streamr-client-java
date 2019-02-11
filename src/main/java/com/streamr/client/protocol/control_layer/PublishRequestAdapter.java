package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageAdapter;

import java.io.IOException;

public class PublishRequestAdapter extends ControlLayerAdapter<PublishRequest> {

    private static final StreamMessageAdapter streamMessageAdapter = new StreamMessageAdapter();

    @Override
    public PublishRequest fromJson(JsonReader reader) throws IOException {
        StreamMessage streamMessage = streamMessageAdapter.fromJson(reader);
        String sessionToken = reader.nextString();
        return new PublishRequest(streamMessage, sessionToken);
    }

    @Override
    public void toJson(JsonWriter writer, PublishRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(PublishRequest.TYPE);
        streamMessageAdapter.toJson(writer, value.getStreamMessage());
        writer.value(value.getSessionToken());
        writer.endArray();
    }
}
