package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

final class ContentAdapter extends JsonAdapter<Map<String, Object>> {
  private final JsonAdapter<Map<String, Object>> adapter =
      new Moshi.Builder()
          .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
          .build()
          .adapter(Types.newParameterizedType(Map.class, String.class, Object.class));

  ContentAdapter() {}

  @Nullable
  @Override
  public Map<String, Object> fromJson(final JsonReader reader) throws IOException {
    return adapter.fromJson(reader);
  }

  @Override
  public void toJson(final JsonWriter writer, @Nullable final Map<String, Object> value)
      throws IOException {
    adapter.toJson(writer, value);
  }
}
