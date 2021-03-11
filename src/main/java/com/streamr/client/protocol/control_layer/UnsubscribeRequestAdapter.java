package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;

final class UnsubscribeRequestAdapter extends ControlLayerAdapter<UnsubscribeRequest> {

  UnsubscribeRequestAdapter() {
    super(UnsubscribeRequest.class);
  }

  @Override
  public UnsubscribeRequest fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    return new UnsubscribeRequest(requestId, streamId, streamPartition);
  }

  @Override
  public void toJson(JsonWriter writer, UnsubscribeRequest value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(UnsubscribeRequest.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    writer.endArray();
  }
}
