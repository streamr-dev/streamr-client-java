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
        // Version and type already read
        String requestId = reader.nextString();
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        MessageRef from = msgRefAdapter.fromJson(reader);
        MessageRef to = msgRefAdapter.fromJson(reader);
        String publisherId = nullSafe(reader, r -> r.nextString());
        String msgChainId = nullSafe(reader, r -> r.nextString());
        String sessionToken = nullSafe(reader, r -> r.nextString());
        return new ResendRangeRequest(requestId, streamId, streamPartition, from, to, publisherId, msgChainId, sessionToken);
    }

    @Override
    public void toJson(JsonWriter writer, ResendRangeRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendRangeRequest.TYPE);
        writer.value(value.getRequestId());
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        msgRefAdapter.toJson(writer, value.getFromMsgRef());
        msgRefAdapter.toJson(writer, value.getToMsgRef());
        writer.value(value.getPublisherId());
        writer.value(value.getMsgChainId());
        writer.value(value.getSessionToken());
        writer.endArray();
    }
}
