package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendResponseResendingAdapter extends ControlLayerAdapter<ResendResponseResending> {

    @Override
    public ResendResponseResending fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        return new ResendResponseResending(streamId, streamPartition, subId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendResponseResending value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendResponseResending.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        writer.endArray();
    }
}
