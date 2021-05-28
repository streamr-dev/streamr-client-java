package com.streamr.client.rest;

import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import com.streamr.client.utils.Address;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

final class Json {
  private Json() {}

  public static Moshi.Builder newMoshiBuilder() {
    return new Moshi.Builder()
        .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
        .add(BigDecimal.class, new BigDecimalAdapter().nullSafe())
        .add(Address.class, new AddressJsonAdapter().nullSafe())
        .add(new InstantJsonAdapter());
  }

  public static String withValue(String json, String key, Object value) throws IOException {
    Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
    JsonAdapter<Map<String,Object>> adapter = Json.newMoshiBuilder().build().adapter(type);
    Map<String,Object> jsonMap;
    try {
      jsonMap = adapter.fromJson(json);
    } catch (JsonDataException e) {
      throw new IOException("Not a JSON object (possibly array)");
    }
    jsonMap.put(key, value);
    return adapter.toJson(jsonMap);
  }
}
