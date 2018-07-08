package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import com.streamr.client.rest.Stream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class StreamrClient {

    public static final Moshi MOSHI = new Moshi.Builder()
            .add(Date.class, new Rfc3339DateJsonAdapter())
            .build();

    public static final JsonAdapter<Stream> streamJsonAdapter = MOSHI.adapter(Stream.class);
            //Types.newParameterizedType(List.class, Contributor.class));

    private StreamrClientOptions options;

    public StreamrClient() {
        options = new StreamrClientOptions();
    }

    public StreamrClient(String apiKey) {
        options = new StreamrClientOptions(apiKey);
    }

    public StreamrClient(StreamrClientOptions options) {
        this.options = options;
    }

    private Request.Builder addAuthenticationHeader(Request.Builder builder, String apiKey) {
        if (apiKey == null) {
            return builder;
        } else {
            return builder.addHeader("Authorization", "token " + apiKey);
        }
    }

    private <T> T get(String endpoint, String apiKey, JsonAdapter<T> adapter) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Create request for remote resource.
        Request request = addAuthenticationHeader(new Request.Builder()
                        .url(options.getRestApiUrl() + endpoint),
                apiKey
        ).build();

        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();

        // Deserialize HTTP response to concrete type.
        ResponseBody body = response.body();
        return adapter.fromJson(body.source());
    }

    /*
     * Stream endpoints
     */
    public Stream getStream(String streamId, String apiKey) throws IOException {
        if (streamId == null) {
            throw new IllegalArgumentException("streamId cannot be null!");
        }

        return get("/streams/" + streamId, apiKey, streamJsonAdapter);
    }

}
