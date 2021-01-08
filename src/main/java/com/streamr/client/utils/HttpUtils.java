package com.streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.exceptions.AuthenticationException;
import com.streamr.client.exceptions.PermissionDeniedException;
import com.streamr.client.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import okhttp3.MediaType;
import okhttp3.Response;

public class HttpUtils {

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

  /** You might have to close {@code response} if {@code assertSuccessful()} fails. */
  public static void assertSuccessful(Response response) throws IOException {
    if (!response.isSuccessful()) {
      String action = response.request().method() + " " + response.request().url().toString();

      switch (response.code()) {
        case HttpURLConnection.HTTP_NOT_FOUND:
          throw new ResourceNotFoundException(action);
        case HttpURLConnection.HTTP_UNAUTHORIZED:
          throw new AuthenticationException(action);
        case HttpURLConnection.HTTP_FORBIDDEN:
          throw new PermissionDeniedException(action);
        default:
          throw new RuntimeException(
              action
                  + " failed with HTTP status "
                  + response.code()
                  + ":"
                  + response.body().string());
      }
    }
  }
}
