package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class UnsubscribeRequestAdapter extends JsonAdapter<UnsubscribeRequest> {

    @Override
    public UnsubscribeRequest fromJson(JsonReader reader) throws IOException {
        // TODO

        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void toJson(JsonWriter writer, UnsubscribeRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(UnsubscribeRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.endArray();
    }
}
