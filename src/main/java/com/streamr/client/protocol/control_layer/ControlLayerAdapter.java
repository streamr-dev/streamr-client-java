package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;

public abstract class ControlLayerAdapter<T extends ControlMessage> extends JsonAdapter<T> {

    public void controlMessagetoJson(JsonWriter writer, ControlMessage value) throws IOException {
        toJson(writer, (T) value);
    }
}
