package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import java.io.IOException;

public class ErrorResponseAdapter extends ControlLayerAdapter<ErrorResponse> {
  ErrorResponseAdapter() {
    super(ErrorResponse.class);
  }

  @Override
  public ErrorResponse fromJson(JsonReader reader) throws IOException {
    // Version and type already read
    String requestId = reader.nextString();
    String errorMessage = reader.nextString();
    String errorCode = reader.nextString();
    return new ErrorResponse(requestId, errorMessage, errorCode);
  }

  @Override
  public void toJson(JsonWriter writer, ErrorResponse value) throws IOException {
    writer.beginArray();
    writer.value(ControlMessage.LATEST_VERSION);
    writer.value(ErrorResponse.TYPE);
    writer.value(value.getRequestId());
    writer.value(value.getErrorMessage());
    writer.value(value.getErrorCode());
    writer.endArray();
  }
}
