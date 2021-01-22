package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;

final class ResendLastRequestAdapter extends ControlLayerAdapter<ResendLastRequest> {

  ResendLastRequestAdapter() {
    super(ResendLastRequest.class);
  }

  @Override
  public ResendLastRequest fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String streamId = reader.nextString();
    int streamPartition = reader.nextInt();
    int numberLast = reader.nextInt();
    String sessionToken = nullSafe(reader, r -> r.nextString());
    return new ResendLastRequest(requestId, streamId, streamPartition, numberLast, sessionToken);
  }

  @Override
  public void toJson(JsonWriter writer, ResendLastRequest value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(ResendLastRequest.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getStreamId());
    writer.value(value.getStreamPartition());
    writer.value(value.getNumberLast());
    writer.value(value.getSessionToken());
    writer.endArray();
  }
}
