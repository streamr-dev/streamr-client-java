package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.util.function.Function;

public abstract class ControlLayerAdapter<T extends ControlMessage> extends JsonAdapter<T> {
    private final Class<T> messageClass;

    ControlLayerAdapter(final Class<T> messageClass) {
        this.messageClass = messageClass;
    }

    public void controlMessagetoJson(JsonWriter writer, ControlMessage value) throws IOException {
        toJson(writer, messageClass.cast(value));
    }

    public <U> U nullSafe(JsonReader reader, CheckedFunction<JsonReader,U> readerFunction) throws IOException {
        if (reader.peek().equals(JsonReader.Token.NULL)) {
            reader.nextNull();
            return null;
        } else {
            return readerFunction.apply(reader);
        }
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
