package com.streamr.client.protocol.control_layer;

import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

public abstract class ControlMessage {
    private static final ControlMessageAdapter adapter = new ControlMessageAdapter();
    private static final Logger log = LogManager.getLogger();

    public static final int LATEST_VERSION = 2;
    private final int type;
    private final String requestId;

    public ControlMessage(int type, String requestId) {
        this.type = type;
        this.requestId = requestId;
    }

    public int getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public String toJson() {
        Buffer buffer = new Buffer();
        JsonWriter writer = JsonWriter.of(buffer);
        try {
            adapter.toJson(writer, this);
            return buffer.readUtf8();
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    public static ControlMessage fromJson(String json) throws IOException {
        JsonReader reader = JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")));
        return adapter.fromJson(reader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlMessage that = (ControlMessage) o;
        return this.toJson().equals(that.toJson());
    }

}
