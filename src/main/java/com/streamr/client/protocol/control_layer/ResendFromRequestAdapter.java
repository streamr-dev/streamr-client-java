package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.MessageRef;

import java.io.IOException;

public class ResendFromRequestAdapter extends JsonAdapter<ResendFromRequest> {

    @Override
    public ResendFromRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        reader.beginArray();
        long timestamp = reader.nextLong();
        long sequenceNumber = reader.nextLong();
        reader.endArray();
        String publisherId = reader.nextString();

        return new ResendFromRequest(streamId, streamPartition, subId, new MessageRef(timestamp, sequenceNumber), publisherId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendFromRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendFromRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        writer.beginArray();
        writer.value(value.getFromMsgRef().getTimestamp());
        writer.value(value.getFromMsgRef().getSequenceNumber());
        writer.endArray();
        writer.value(value.getPublisherId());
        writer.endArray();
    }
}
