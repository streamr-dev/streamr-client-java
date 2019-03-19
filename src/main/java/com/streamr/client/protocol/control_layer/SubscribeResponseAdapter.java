package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class SubscribeResponseAdapter extends ControlLayerAdapter<SubscribeResponse> {

    @Override
    public SubscribeResponse fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        return new SubscribeResponse(streamId, streamPartition);
    }

    @Override
    public void toJson(JsonWriter writer, SubscribeResponse value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(SubscribeResponse.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.endArray();
    }
}
