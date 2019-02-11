package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.MessageRefAdapter;

import java.io.IOException;

public class ResendRangeRequestAdapter extends ControlLayerAdapter<ResendRangeRequest> {
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    @Override
    public ResendRangeRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        MessageRef from = msgRefAdapter.fromJson(reader);
        MessageRef to = msgRefAdapter.fromJson(reader);
        String publisherId = reader.nextString();
        return new ResendRangeRequest(streamId, streamPartition, subId, from, to, publisherId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendRangeRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendRangeRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        msgRefAdapter.toJson(writer, value.getFromMsgRef());
        msgRefAdapter.toJson(writer, value.getToMsgRef());
        writer.value(value.getPublisherId());
        writer.endArray();
    }
}
