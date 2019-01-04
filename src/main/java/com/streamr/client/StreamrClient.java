package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;
import com.streamr.client.exceptions.AuthenticationRequiredException;
import com.streamr.client.exceptions.PermissionDeniedException;
import com.streamr.client.exceptions.ResourceNotFoundException;
import com.streamr.client.rest.Stream;
import okhttp3.*;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * This class exposes the main set of methods available for application developers.
 * It inherits the functionality in StreamrWebsocketClient and adds methods for interacting
 * with the RESTful API endpoints.
 */
public class StreamrClient extends StreamrWebsocketClient {

    public static final JsonAdapter<Stream> streamJsonAdapter = MOSHI.adapter(Stream.class);
    public static final JsonAdapter<List<Stream>> streamListJsonAdapter = MOSHI.adapter(Types.newParameterizedType(List.class, Stream.class));

    private static final MediaType jsonType = MediaType.parse("application/json");

    /**
     * Creates a StreamrClient with default options
     */
    public StreamrClient() {
        super(new StreamrClientOptions());
    }

    public StreamrClient(String apiKey) {
        this(new StreamrClientOptions(apiKey));
    }

    public StreamrClient(StreamrClientOptions options) {
        super(options);
    }

    /*
     * Helper functions
     */

    private Request.Builder addAuthenticationHeader(Request.Builder builder, String apiKey) {
        if (apiKey == null) {
            return builder;
        } else {
            return builder.addHeader("Authorization", "token " + apiKey);
        }
    }

    private static void assertSuccessful(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String action = response.request().method() + " " + response.request().url().toString();

            switch (response.code()) {
                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new ResourceNotFoundException(action);
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new AuthenticationRequiredException(action);
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

    private <T> T execute(Request request, JsonAdapter<T> adapter) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();
        assertSuccessful(response);

        // Deserialize HTTP response to concrete type.
        return adapter.fromJson(response.body().source());
    }

    private <T> T get(String endpoint, String apiKey, JsonAdapter<T> adapter) throws IOException {
        Request request = addAuthenticationHeader(new Request.Builder()
                        .url(options.getRestApiUrl() + endpoint),
                apiKey
        ).build();

        return execute(request, adapter);
    }

    private <T> T post(String endpoint, String requestBody, String apiKey, JsonAdapter<T> adapter) throws IOException {
        Request request = addAuthenticationHeader(new Request.Builder()
                        .url(options.getRestApiUrl() + endpoint)
                        .post(RequestBody.create(jsonType, requestBody)),
                apiKey
        ).build();

        return execute(request, adapter);
    }

    /*
     * Stream endpoints
     */

    public Stream getStream(String streamId) throws IOException, ResourceNotFoundException {
        if (streamId == null) {
            throw new IllegalArgumentException("streamId cannot be null!");
        }

        return get("/streams/" + streamId, options.getApiKey(), streamJsonAdapter);
    }

    public Stream createStream(Stream stream) throws IOException {
        if (stream.getName() == null) {
            throw new IllegalArgumentException("The stream name must be set!");
        }

        return post("/streams", streamJsonAdapter.toJson(stream), options.getApiKey(), streamJsonAdapter);
    }

}
