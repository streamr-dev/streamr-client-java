package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendResponseNoResendAdapter extends ControlLayerAdapter<ResendResponseNoResend> {

    @Override
    public ResendResponseNoResend fromJson(JsonReader reader) throws IOException {
        // Version and type already read
        String requestId = reader.nextString();
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        return new ResendResponseNoResend(requestId, streamId, streamPartition);
    }

    @Override
    public void toJson(JsonWriter writer, ResendResponseNoResend value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendResponseNoResend.TYPE);
        writer.value(value.getRequestId());
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.endArray();
    }
}
