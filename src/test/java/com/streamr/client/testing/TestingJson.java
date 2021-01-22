package com.streamr.client.testing;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import com.streamr.client.rest.BigDecimalAdapter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public final class TestingJson {
  private TestingJson() {}

  public static String toJson(final Map<String, Object> payload) {
    final ParameterizedType parameterizedType =
        Types.newParameterizedType(Map.class, String.class, Object.class);
    return new Moshi.Builder()
        .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
        .add(BigDecimal.class, new BigDecimalAdapter().nullSafe())
        // TODO .add(new InstantJsonAdapter())
        .build()
        .adapter(parameterizedType)
        .toJson(payload);
  }

  public static Map<String, Object> fromJson(final String json) throws IOException {
    final ParameterizedType parameterizedType =
        Types.newParameterizedType(Map.class, String.class, Object.class);
    return new Moshi.Builder()
        .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
        .add(BigDecimal.class, new BigDecimalAdapter().nullSafe())
        // TODO .add(new InstantJsonAdapter())
        .build()
        .<Map<String, Object>>adapter(parameterizedType)
        .fromJson(json);
  }
}
