package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import javax.annotation.Nullable;
import java.io.IOException;

public class DeleteRequestAdapter extends ControlLayerAdapter<DeleteRequest> {
    @Nullable
    @Override
    public DeleteRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        Long fromTimestamp = nullSafe(reader, JsonReader::nextLong);
        Long toTimestamp = nullSafe(reader, JsonReader::nextLong);
        return new DeleteRequest(streamId, streamPartition, fromTimestamp, toTimestamp);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable DeleteRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(DeleteRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getFromTimestamp());
        writer.value(value.getToTimestamp());
        writer.endArray();
    }
}
