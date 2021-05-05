package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;

final class ResendResponseResentAdapter extends ControlLayerAdapter<ResendResponseResent> {

  ResendResponseResentAdapter() {
    super(ResendResponseResent.class);
  }

  @Override
  public ResendResponseResent fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    return new ResendResponseResent(requestId, streamId, streamPartition);
  }

  @Override
  public void toJson(JsonWriter writer, ResendResponseResent value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(ResendResponseResent.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    writer.endArray();
  }
}
