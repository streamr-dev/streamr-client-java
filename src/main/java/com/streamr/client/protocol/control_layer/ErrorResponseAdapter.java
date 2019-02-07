package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public class ErrorResponseAdapter extends JsonAdapter<ErrorResponse> {

    @Override
    public ErrorResponse fromJson(JsonReader reader) throws IOException {
        String errorMessage = reader.nextString();
        return new ErrorResponse(errorMessage);
    }

    @Override
    public void toJson(JsonWriter writer, ErrorResponse value) throws IOException {
        writer.beginArray();
        writer.value(ControlMessage.LATEST_VERSION);
        writer.value(ErrorResponse.TYPE);
        writer.value(value.getErrorMessage());
        writer.endArray();
    }
}
