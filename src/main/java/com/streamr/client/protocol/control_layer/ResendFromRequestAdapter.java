package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.MessageRefAdapter;
import java.io.IOException;

public class ResendFromRequestAdapter extends ControlLayerAdapter<ResendFromRequest> {
  private static final MessageRefAdapter msgRefAdapter = new MessageRefAdapter();

  ResendFromRequestAdapter() {
    super(ResendFromRequest.class);
  }

  @Override
  public ResendFromRequest fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    MessageRef from = msgRefAdapter.fromJson(reader);
    String publisherId = nullSafe(reader, r -> r.nextString());
    String sessionToken = nullSafe(reader, r -> r.nextString());
    return new ResendFromRequest(
        requestId, streamId, streamPartition, from, publisherId, sessionToken);
  }

  @Override
  public void toJson(JsonWriter writer, ResendFromRequest value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(ResendFromRequest.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    msgRefAdapter.toJson(writer, value.getFromMsgRef());
    writer.value(value.getPublisherId());
    writer.value(value.getSessionToken());
    writer.endArray();
  }
}
