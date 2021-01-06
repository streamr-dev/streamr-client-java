package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendResponseResendingAdapter extends ControlLayerAdapter<ResendResponseResending> {

    ResendResponseResendingAdapter() {
        super(ResendResponseResending.class);
    }

    @Override
    public ResendResponseResending fromJson(JsonReader reader) throws IOException {
        // Version and type already read
        String requestId = reader.nextString();
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        return new ResendResponseResending(requestId, streamId, streamPartition);
    }

    @Override
    public void toJson(JsonWriter writer, ResendResponseResending value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendResponseResending.TYPE);
        writer.value(value.getRequestId());
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.endArray();
    }
}
