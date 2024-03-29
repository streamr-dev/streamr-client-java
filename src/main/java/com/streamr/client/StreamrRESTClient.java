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
import static com.streamr.client.utils.Web3jUtils.waitForCondition;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;


class StorageNodeInput {
    Address storageNodeAddress;
}

/**
 * This class exposes the RESTful API endpoints.
 */
public abstract class StreamrRESTClient extends AbstractStreamrClient {

    public static final JsonAdapter<Stream> streamJsonAdapter = MOSHI.adapter(Stream.class);
    public static final JsonAdapter<Permission> permissionJsonAdapter = MOSHI.adapter(Permission.class);
    public static final JsonAdapter<UserInfo> userInfoJsonAdapter = MOSHI.adapter(UserInfo.class);
    public static final JsonAdapter<Publishers> publishersJsonAdapter = MOSHI.adapter(Publishers.class);
    public static final JsonAdapter<Subscribers> subscribersJsonAdapter = MOSHI.adapter(Subscribers.class);
    public static final JsonAdapter<StorageNode> storageNodeJsonAdapter = MOSHI.adapter(StorageNode.class);
    public static final JsonAdapter<List<Stream>> streamListJsonAdapter = MOSHI.adapter(Types.newParameterizedType(List.class, Stream.class));
    public static final JsonAdapter<Secret> secretJsonAdapter = MOSHI.adapter(Secret.class);
    public static final JsonAdapter<Product> productJsonAdapter = MOSHI.adapter(Product.class);


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

    protected OkHttpClient.Builder httpClientBuilder(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if(options != null) {
            builder.connectTimeout(options.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            builder.readTimeout(options.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
            builder.writeTimeout(options.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
        return builder;
    }

    private <T> T execute(Request request, JsonAdapter<T> adapter) throws IOException {
        OkHttpClient client = new OkHttpClient(httpClientBuilder());
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

    private <T> T delete(HttpUrl url) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).delete();
        return executeWithRetry(builder, null, true);
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

    public void addStreamToStorageNode(String streamId, StorageNode storageNode) throws Exception {
        // currently we support only one storage node
        // -> we can validate that the given address is that address
        // -> remove this comparison when we start to support multiple storage nodes
        if (!storageNode.getAddress().equals(this.options.getStorageNodeAddress())) {
            throw new IllegalArgumentException("Unknown storage node: " + storageNode.getAddress());
        }
        HttpUrl url = getEndpointUrl("streams", streamId, "storageNodes");
        post(url, storageNodeJsonAdapter.toJson(storageNode), streamJsonAdapter);
        // wait for propagation: the storage node sees the database change in E&E and
        // is ready to store the any stream data which we publish
        final int POLL_INTERVAL = 500;
        final int TIMEOUT = 30 * 1000;
        waitForCondition(() -> {
            return this.isStreamStoredInStorageNode(streamId) ? true : null;
        }, POLL_INTERVAL, TIMEOUT, new IOException("Propagation timeout when adding stream to a storage node: " + streamId));
    }

    private boolean isStreamStoredInStorageNode(String streamId) throws IOException {
        final int PARTITION = 0;
        HttpUrl.Builder urlBuilder = HttpUrl.parse(this.options.getStorageNodeUrl() + "/api/v1").newBuilder();
        Arrays.asList("streams", streamId, "storage", "partitions", String.valueOf(PARTITION))
            .forEach(segment -> urlBuilder.addPathSegment(segment));
        Request request = new Request.Builder().url(urlBuilder.build()).build();
        Response response = new OkHttpClient().newCall(request).execute();
        int statusCode = response.code();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            return true;
        } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        } else {
            throw new IOException("Unexpected response code " + statusCode + " when fetching stream storage status");
        }
    }

    public void removeStreamFromStorageNode(String streamId, StorageNode storageNode) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "storageNodes", storageNode.getAddress().toString());
        delete(url);
    }

    public List<StorageNode> getStorageNodes(String streamId) throws IOException {
        HttpUrl url = getEndpointUrl("streams", streamId, "storageNodes");
        JsonAdapter<List<StorageNodeInput>> adapter = MOSHI.adapter(Types.newParameterizedType(List.class, StorageNodeInput.class));
        List<StorageNodeInput> items = get(url, adapter);
        return items.stream()
            .map(item -> new StorageNode(item.storageNodeAddress))
            .collect(Collectors.toList());
    }

    public List<StreamPart> getStreamPartsByStorageNode(StorageNode storageNode) throws IOException {
        HttpUrl url = getEndpointUrl("storageNodes", storageNode.getAddress().toString(), "streams");
        List<Stream> streams = get(url, streamListJsonAdapter);
        return streams.stream()
            .map(stream -> stream.toStreamParts())
            .flatMap(x -> x.stream())
            .collect(Collectors.toList());
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

    public Secret setDataUnionSecret(String DUMainnetAddress, String secretName) throws IOException {
        HttpUrl url = getEndpointUrl("dataunions", DUMainnetAddress, "secrets");
        String json = String.format("{ %s : \"%s\" }", "name", secretName);
        return post(url, json, secretJsonAdapter);
    }

    public void requestDataUnionJoin(String DUMainnetAddress, String memberAddress, String secret) throws IOException {
        HttpUrl url = getEndpointUrl("dataunions", DUMainnetAddress, "joinRequests");
        String json = String.format("{ %s : \"%s\", %s : \"%s\" }", "memberAddress", memberAddress, "secret", secret);
        post(url, json, null);
    }

    /**
     * for testing, not publicly usable
     * @param p
     * @throws IOException
     */

    void createProduct(Product p) throws IOException {
        HttpUrl url = getEndpointUrl("products");
        post(url, productJsonAdapter.toJson(p), null);
    }


}
