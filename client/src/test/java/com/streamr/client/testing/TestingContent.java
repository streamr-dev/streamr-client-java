package com.streamr.client.testing;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class TestingContent {
  private TestingContent() {}

  private static JsonAdapter<Map<String, Object>> newContentAdapter() {
    return new Moshi.Builder()
        .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
        .build()
        .<Map<String, Object>>adapter(
            Types.newParameterizedType(Map.class, String.class, Object.class));
  }

  public static StreamMessage.Content fromJsonString(final String json) {
    final byte[] payload = json.getBytes(StandardCharsets.UTF_8);
    return StreamMessage.Content.Factory.withJsonAsPayload(payload);
  }

  public static StreamMessage.Content fromJsonMap(final Map<String, Object> map) {
    final JsonAdapter<Map<String, Object>> adapter = newContentAdapter();
    final String json = adapter.toJson(map);
    return fromJsonString(json);
  }

  public static StreamMessage.Content emptyMessage() {
    final Map<String, Object> content = Collections.unmodifiableMap(new HashMap<String, Object>());
    return fromJsonMap(content);
  }
}
