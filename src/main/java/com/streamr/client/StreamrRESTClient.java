package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.exceptions.AmbiguousResultsException;
import com.streamr.client.exceptions.AuthenticationException;
import com.streamr.client.exceptions.ResourceNotFoundException;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.rest.*;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.utils.Address;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

/**
 * This class exposes the RESTful API endpoints.
 */
public abstract class StreamrRESTClient extends AbstractStreamrClient {

    public static final JsonAdapter<Stream> streamJsonAdapter = MOSHI.adapter(Stream.class);
    public static final JsonAdapter<Permission> permissionJsonAdapter = MOSHI.adapter(Permission.class);
    public static final JsonAdapter<UserInfo> userInfoJsonAdapter = MOSHI.adapter(UserInfo.class);
    public static final JsonAdapter<Publishers> publishersJsonAdapter = MOSHI.adapter(Publishers.class);
    public static final JsonAdapter<Subscribers> subscribersJsonAdapter = MOSHI.adapter(Subscribers.class);
    public static final JsonAdapter<List<Stream>> streamListJsonAdapter = MOSHI.adapter(Types.newParameterizedType(List.class, Stream.class));

    // private final Publisher publisher;

    /**
     * Creates a StreamrClient with default options
     */
    public StreamrRESTClient() {
        this(new StreamrClientOptions());
    }

    public StreamrRESTClient(AuthenticationMethod authenticationMethod) {
        this(new StreamrClientOptions(authenticationMethod));
    }

    public StreamrRESTClient(StreamrClientOptions options) {
        super(options);
        // publisher = new Publisher(this);
    }

    /*
     * Helper functions
     */

    private Request.Builder addAuthenticationHeader(Request.Builder builder, boolean newToken) {
        if (!session.isAuthenticated()) {
            return builder;
        } else {
            String sessionToken = newToken ? session.getNewSessionToken() : session.getSessionToken();
            builder.removeHeader("Authorization");
            return builder.addHeader("Authorization", "Bearer " + sessionToken);
        }
    }

    private <T> T execute(Request request, JsonAdapter<T> adapter) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();
        try {
            HttpUtils.assertSuccessful(response);

            // Deserialize HTTP response to concrete type.
            return adapter == null ? null : adapter.fromJson(response.body().source());
        } finally {
            response.close();
        }
    }

    private <T> T executeWithRetry(Request.Builder builder, JsonAdapter<T> adapter, boolean retryIfSessionExpired) throws IOException {
        Request request = addAuthenticationHeader(builder, false).build();
        try {
            return execute(request, adapter);
        } catch (AuthenticationException e) {
            if (retryIfSessionExpired) {
                Request request2 = addAuthenticationHeader(builder, true).build();
                return execute(request2, adapter);
            } else {
                throw e;
            }
        }
    }

    private <T> T get(HttpUrl url, JsonAdapter<T> adapter) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        return executeWithRetry(builder, adapter, true);
    }

    private <T> T post(HttpUrl url, String requestBody, JsonAdapter<T> adapter) throws IOException {
        return post(url, requestBody, adapter, true);
    }

    private <T> T post(HttpUrl url, String requestBody, JsonAdapter<T> adapter, boolean retryIfSessionExpired) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(HttpUtils.jsonType, requestBody));
        return executeWithRetry(builder, adapter, retryIfSessionExpired);
    }

    /*
     * Stream endpoints
     */

    public Stream getStream(String streamId) throws IOException, ResourceNotFoundException {
        if (streamId == null) {
            throw new IllegalArgumentException("streamId cannot be null!");
        }

        HttpUrl url = getEndpointUrl("streams", streamId);
        return get(url, streamJsonAdapter);
    }

    public Stream getStreamByName(String name) throws IOException, AmbiguousResultsException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Stream name must be specified!");
        }

        HttpUrl url = getEndpointUrl("streams")
                .newBuilder()
                .setQueryParameter("name", name)
                .build();

        List<Stream> matches = get(url, streamListJsonAdapter);
        if (matches.size() == 1) {
            return matches.get(0);
        } else if (matches.isEmpty()) {
            throw new ResourceNotFoundException("stream by name: " + name);
        } else {
            throw new AmbiguousResultsException("Name is not unique! Multiple streams found by name: " + name);
        }
    }

    public Stream createStream(Stream stream) throws IOException {
        HttpUrl url = getEndpointUrl("streams");
        return post(url, streamJsonAdapter.toJson(stream), streamJsonAdapter);
    }

    public Permission grant(Stream stream, Permission.Operation operation, String user) throws IOException {
        if (stream == null || operation == null || user == null) {
            throw new IllegalArgumentException("Must give all of stream, operation, and user!");
        }

        Permission permission = new Permission(operation, user);

        HttpUrl url = getEndpointUrl("streams", stream.getId(), "permissions");
        return post(url, permissionJsonAdapter.toJson(permission), permissionJsonAdapter);
    }

    public Permission grantPublic(Stream stream, Permission.Operation operation) throws IOException {
        if (stream == null || operation == null) {
            throw new IllegalArgumentException("Must give stream and operation!");
        }

        Permission permission = new Permission(operation);

        HttpUrl url = getEndpointUrl("streams", stream.getId(), "permissions");
        return post(url, permissionJsonAdapter.toJson(permission), permissionJsonAdapter);
    }

    public UserInfo getUserInfo() throws IOException {
        HttpUrl url = getEndpointUrl("users", "me");
        return get(url, userInfoJsonAdapter);
    }

    public List<String> getPublishers(String streamId) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "publishers");
        return get(url, publishersJsonAdapter).getAddresses();
    }

    public boolean isPublisher(String streamId, Address address) throws IOException {
        return isPublisher(streamId, address.toString());
    }

    public boolean isPublisher(String streamId, String ethAddress) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "publisher", ethAddress);
        try {
            get(url, null);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public List<String> getSubscribers(String streamId) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "subscribers");
        return get(url, subscribersJsonAdapter).getAddresses();
    }

    public boolean isSubscriber(String streamId, Address address) throws IOException {
        return isSubscriber(streamId, address.toString());
    }

    public boolean isSubscriber(String streamId, String ethAddress) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "subscriber", ethAddress);
        try {
            get(url, null);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public void logout() throws IOException {
        HttpUrl url = getEndpointUrl("logout");
        post(url, "", null, false);
    }

    private HttpUrl getEndpointUrl(String... pathSegments) {
        HttpUrl.Builder builder = HttpUrl.parse(options.getRestApiUrl()).newBuilder();
        for (String segment : pathSegments) {
            builder = builder.addPathSegment(segment);
        }
        return builder.build();
    }
}
