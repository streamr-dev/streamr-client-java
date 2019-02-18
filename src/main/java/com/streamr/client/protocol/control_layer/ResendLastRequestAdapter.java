package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ResendLastRequestAdapter extends ControlLayerAdapter<ResendLastRequest> {

    @Override
    public ResendLastRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        int numberLast = reader.nextInt();
        String sessionToken = reader.nextString();
        return new ResendLastRequest(streamId, streamPartition, subId, numberLast, sessionToken);
    }

    @Override
    public void toJson(JsonWriter writer, ResendLastRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendLastRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        writer.value(value.getNumberLast());
        writer.value(value.getSessionToken());
        writer.endArray();
    }
}