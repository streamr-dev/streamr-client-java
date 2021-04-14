package com.streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.streamr.client.exceptions.AuthenticationException;
import com.streamr.client.exceptions.PermissionDeniedException;
import com.streamr.client.exceptions.ResourceNotFoundException;
import okhttp3.MediaType;
import okhttp3.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class HttpUtils {

    static public Function<Moshi.Builder, Moshi.Builder> addDefaultAdapters = (builder) -> builder
            .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
            .add(Address.class, new AddressJsonAdapter().nullSafe())
            .add(BigDecimal.class, new BigDecimalAdapter().nullSafe());

    // Thread safe
    public static final Moshi MOSHI = addDefaultAdapters.apply(new Moshi.Builder()).build();

    public static final JsonAdapter<Map> mapAdapter = MOSHI.adapter(Map.class);
    public static final JsonAdapter<List> listAdapter = MOSHI.adapter(List.class);

    public static final MediaType jsonType = MediaType.parse("application/json");

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
                    throw new RuntimeException(action
                            + " failed with HTTP status "
                            + response.code()
                            + ":" + response.body().string());
            }
        }
    }
}
