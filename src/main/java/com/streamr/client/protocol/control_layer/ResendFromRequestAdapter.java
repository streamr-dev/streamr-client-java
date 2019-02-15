package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.MessageRefAdapter;

import java.io.IOException;

public class ResendFromRequestAdapter extends ControlLayerAdapter<ResendFromRequest> {
    private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

    @Override
    public ResendFromRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        MessageRef from = msgRefAdapter.fromJson(reader);
        String publisherId = reader.nextString();
        String msgChainId = reader.nextString();
        String sessionToken = reader.nextString();
        return new ResendFromRequest(streamId, streamPartition, subId, from, publisherId, msgChainId, sessionToken);
    }

    @Override
    public void toJson(JsonWriter writer, ResendFromRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendFromRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        msgRefAdapter.toJson(writer, value.getFromMsgRef());
        writer.value(value.getPublisherId());
        writer.value(value.getMsgChainId());
        writer.value(value.getSessionToken());
        writer.endArray();
    }
}
