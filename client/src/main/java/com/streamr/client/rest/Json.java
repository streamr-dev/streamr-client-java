package com.streamr.client.rest;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.JsonAdapter;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import com.streamr.client.utils.Address;

import java.io.IOException;
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

  public static String withValue(String json, String key, String value) throws IOException {
    JsonAdapter<Object> adapter = Json.newMoshiBuilder().build().adapter(Object.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> jsonObject = (Map<String,Object>) adapter.fromJson(json);
    jsonObject.put(key, value);
    return adapter.toJson(jsonObject);
  }
}
