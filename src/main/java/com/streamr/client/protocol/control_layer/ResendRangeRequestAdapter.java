package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.MessageRef;

import java.io.IOException;

public class ResendRangeRequestAdapter extends JsonAdapter<ResendRangeRequest> {

    @Override
    public ResendRangeRequest fromJson(JsonReader reader) throws IOException {
        String streamId = reader.nextString();
        int streamPartition = reader.nextInt();
        String subId = reader.nextString();
        reader.beginArray();
        long fromTimestamp = reader.nextLong();
        long fromSequenceNumber = reader.nextLong();
        reader.endArray();
        reader.beginArray();
        long toTimestamp = reader.nextLong();
        long toSequenceNumber = reader.nextLong();
        reader.endArray();
        String publisherId = reader.nextString();

        return new ResendRangeRequest(streamId, streamPartition, subId,
                new MessageRef(fromTimestamp, fromSequenceNumber), new MessageRef(toTimestamp, toSequenceNumber), publisherId);
    }

    @Override
    public void toJson(JsonWriter writer, ResendRangeRequest value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ResendRangeRequest.TYPE);
        writer.value(value.getStreamId());
        writer.value(value.getStreamPartition());
        writer.value(value.getSubId());
        writer.beginArray();
        writer.value(value.getFromMsgRef().getTimestamp());
        writer.value(value.getFromMsgRef().getSequenceNumber());
        writer.endArray();
        writer.beginArray();
        writer.value(value.getToMsgRef().getTimestamp());
        writer.value(value.getToMsgRef().getSequenceNumber());
        writer.endArray();
        writer.value(value.getPublisherId());
        writer.endArray();
    }
}
