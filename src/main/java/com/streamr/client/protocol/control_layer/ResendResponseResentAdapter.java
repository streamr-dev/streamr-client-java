package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendResponseResentAdapter extends ControlLayerAdapter<ResendResponseResent> {

    @Override
    public ResendResponseResent fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String requestId = reader.nextString();
        return new ResendResponseResent(streamId, streamPartition, requestId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendResponseResent value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendResponseResent.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getRequestId());
        writer.endArray();
    }
}
