package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import javax.annotation.Nullable;
import java.io.IOException;

public class DeleteResponseAdapter extends ControlLayerAdapter<DeleteResponse> {
    @Nullable
    @Override
    public DeleteResponse fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String requestId = reader.nextString();
        boolean status = reader.nextBoolean();
        return new DeleteResponse(streamId, streamPartition, requestId, status);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable DeleteResponse value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(DeleteResponse.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getRequestId());
        writer.value(value.getStatus());
        writer.endArray();
    }
}
