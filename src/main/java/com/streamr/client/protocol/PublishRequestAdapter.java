package com.streamr.client.protocol;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.streamr.client.exceptions.UnsupportedPayloadException;

import java.io.IOException;
import java.util.Map;

/*
{
  "type": "publish",
  "stream": "streamId",
  "authKey": "authKey",
  "msg": "{}",                 // the message as stringified json
  "ts": 1533924184016,         // timestamp (optional), defaults to current time on server
  "pkey": "deviceId"           // partition key (optional), defaults to none (random partition)
}
 */
public class PublishRequestAdapter extends JsonAdapter<PublishRequest> {

    // Thread safe
    private static final Moshi moshi = new Moshi.Builder().build();
    private static final JsonAdapter<Map> mapAdapter = moshi.adapter(Map.class);

    @Override
    public PublishRequest fromJson(JsonReader reader) throws IOException {
        // TODO
        throw new RuntimeException("Unimplemented!");
    }

    @Override
    public void toJson(JsonWriter writer, PublishRequest value) throws IOException {
        writer.beginObject();

        writer.name("type").value(value.getType());
        writer.name("stream").value(value.getStreamId());

        if (value.getPayload() instanceof Map) {
            writer.name("msg").value(mapAdapter.toJson((Map) value.getPayload()));
        } else {
            throw new UnsupportedPayloadException("Unsupported message type: " + value.getPayload().getClass()
                    + " (current implementation only supports Map payloads");
        }

        // Optional fields

        if (value.getSessionToken() != null) {
            writer.name("sessionToken").value(value.getSessionToken());
        }
        if (value.getTimestamp() != null) {
            writer.name("ts").value(value.getTimestamp().getTime());
        }
        if (value.getPartitionKey() != null) {
            writer.name("pkey").value(value.getPartitionKey());
        }

        writer.endObject();
    }
}
