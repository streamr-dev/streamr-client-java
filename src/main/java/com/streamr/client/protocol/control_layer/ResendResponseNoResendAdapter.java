package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendResponseNoResendAdapter extends ControlLayerAdapter<ResendResponseNoResend> {

    @Override
    public ResendResponseNoResend fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        return new ResendResponseNoResend(streamId, streamPartition, subId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendResponseNoResend value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendResponseNoResend.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        writer.endArray();
    }
}
