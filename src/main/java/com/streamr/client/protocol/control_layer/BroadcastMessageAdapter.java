package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageAdapter;

import java.io.IOException;

public class BroadcastMessageAdapter extends ControlLayerAdapter<BroadcastMessage>{

    private static final StreamMessageAdapter streamMessageAdapter = new StreamMessageAdapter();

    @Override
    public BroadcastMessage fromJson(JsonReader reader) throws IOException {
        StreamMessage streamMessage = streamMessageAdapter.fromJson(reader);
        return new BroadcastMessage(streamMessage);
    }

    @Override
    public void toJson(JsonWriter writer, BroadcastMessage value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(BroadcastMessage.TYPE);
        streamMessageAdapter.toJson(writer, value.getStreamMessage());
        writer.endArray();
    }
}
