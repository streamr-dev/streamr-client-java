package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class UnsubscribeResponseAdapter extends ControlLayerAdapter<UnsubscribeResponse> {

    @Override
    public UnsubscribeResponse fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        return new UnsubscribeResponse(streamId, streamPartition);
    }

    @Override
    public void toJson(JsonWriter writer, UnsubscribeResponse value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(UnsubscribeResponse.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.endArray();
    }
}
