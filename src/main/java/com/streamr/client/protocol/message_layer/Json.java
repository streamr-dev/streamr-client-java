package com.streamr.client.protocol.message_layer;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import okhttp3.MediaType;

public class Json {
  public static Function<Moshi.Builder, Moshi.Builder> addDefaultAdapters =
      (builder) ->
          builder
              .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
              .add(BigDecimal.class, new BigDecimalAdapter().nullSafe())
              .add(new InstantJsonAdapter());

  // Thread safe
  public static final Moshi MOSHI = addDefaultAdapters.apply(new Moshi.Builder()).build();

  public static final JsonAdapter<Map<String, Object>> mapAdapter =
      MOSHI.adapter(Types.newParameterizedType(Map.class, String.class, Object.class));
  public static final JsonAdapter<List<String>> listAdapter =
      MOSHI.adapter(Types.newParameterizedType(List.class, String.class));

  public static final MediaType jsonType = MediaType.parse("application/json");
}
