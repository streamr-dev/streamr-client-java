package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;

public class SubscribeRequestAdapter extends ControlLayerAdapter<SubscribeRequest> {

  SubscribeRequestAdapter() {
    super(SubscribeRequest.class);
  }

  @Override
  public SubscribeRequest fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    String sessionToken = nullSafe(reader, r -> r.nextString());
    return new SubscribeRequest(requestId, streamId, streamPartition, sessionToken);
  }

  @Override
  public void toJson(JsonWriter writer, SubscribeRequest value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(SubscribeRequest.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    writer.value(value.getSessionToken());
    writer.endArray();
  }
}
