package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonReader;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.Charset;

public class StreamMessageFactory {
    private static StreamMessageAdapter adapter = new StreamMessageAdapter();
    private static JsonReader toReader(String json) {
        return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")));
    }
    public static StreamMessage fromJson(String json) throws IOException {
        return adapter.fromJson(toReader(json));
    }
}
