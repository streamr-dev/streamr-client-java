package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.utils.Address;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.HttpURLConnection;
import java.util.List;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/** This class exposes the RESTful API endpoints. */
public abstract class StreamrRestClient extends AbstractStreamrClient {
  private final JsonAdapter<Stream> streamJsonAdapter;
  private final JsonAdapter<Permission> permissionJsonAdapter;
  private final JsonAdapter<UserInfo> userInfoJsonAdapter;
  private final JsonAdapter<Publishers> publishersJsonAdapter;
  private final JsonAdapter<Subscribers> subscribersJsonAdapter;
  private final JsonAdapter<List<Stream>> streamListJsonAdapter;

  {
    final Moshi moshi = Json.newMoshiBuilder().build();
    streamJsonAdapter = moshi.adapter(Stream.class);
    permissionJsonAdapter = moshi.adapter(Permission.class);
    userInfoJsonAdapter = moshi.adapter(UserInfo.class);
    publishersJsonAdapter = moshi.adapter(Publishers.class);
    subscribersJsonAdapter = moshi.adapter(Subscribers.class);
    final ParameterizedType pt = Types.newParameterizedType(List.class, Stream.class);
    streamListJsonAdapter = moshi.adapter(pt);
  }

  public StreamrRestClient(StreamrClientOptions options) {
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

  private <T> T execute(Request request, JsonAdapter<T> adapter) throws IOException {
    OkHttpClient client = new OkHttpClient();

    // Execute the request and retrieve the response.
    final Call call = client.newCall(request);
    final Response response = call.execute();
    try {
      assertSuccessful(response);

      // Deserialize HTTP response to concrete type.
      if (adapter == null) {
        return null;
      } else {
        final ResponseBody body = response.body();
        BufferedSource source = null;
        if (body != null) {
          source = body.source();
        }
        T result = null;
        if (source != null) {
          result = adapter.fromJson(source);
        }
        return result;
      }
    } finally {
      response.close(); // closing response will also close source and body
    }
  }

  private <T> T executeWithRetry(
      Request.Builder builder, JsonAdapter<T> adapter, boolean retryIfSessionExpired)
      throws IOException {
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

  private <T> T post(
      HttpUrl url, String requestBody, JsonAdapter<T> adapter, boolean retryIfSessionExpired)
      throws IOException {
    final MediaType contentTypeJson = MediaType.parse("application/json");
    final RequestBody content = RequestBody.create(requestBody, contentTypeJson);
    Request.Builder builder = new Request.Builder().url(url).post(content);
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

    HttpUrl url = getEndpointUrl("streams").newBuilder().setQueryParameter("name", name).build();

    List<Stream> matches = get(url, streamListJsonAdapter);
    if (matches.size() == 1) {
      return matches.get(0);
    } else if (matches.isEmpty()) {
      throw new ResourceNotFoundException("stream by name: " + name);
    } else {
      throw new AmbiguousResultsException(
          "Name is not unique! Multiple streams found by name: " + name);
    }
  }

  public Stream createStream(final Stream stream) throws IOException {
    HttpUrl url = getEndpointUrl("streams");
    return post(url, streamJsonAdapter.toJson(stream), streamJsonAdapter);
  }

  public Permission grant(
      final Stream stream, final Permission.Operation operation, final String user)
      throws IOException {
    if (stream == null || operation == null || user == null) {
      throw new IllegalArgumentException("Must give all of stream, operation, and user!");
    }

    Permission permission = new Permission(operation, user);

    HttpUrl url = getEndpointUrl("streams", stream.getId(), "permissions");
    return post(url, permissionJsonAdapter.toJson(permission), permissionJsonAdapter);
  }

  public Permission grantPublic(final Stream stream, final Permission.Operation operation)
      throws IOException {
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

  public List<String> getPublishers(final String streamId) throws IOException {
    HttpUrl url = getEndpointUrl("streams", streamId, "publishers");
    return get(url, publishersJsonAdapter).getAddresses();
  }

  public boolean isPublisher(final String streamId, final Address address) throws IOException {
    return isPublisher(streamId, address.toString());
  }

  public boolean isPublisher(final String streamId, final String ethAddress) throws IOException {
    HttpUrl url = getEndpointUrl("streams", streamId, "publisher", ethAddress);
    try {
      get(url, null);
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  public List<String> getSubscribers(final String streamId) throws IOException {
    HttpUrl url = getEndpointUrl("streams", streamId, "subscribers");
    return get(url, subscribersJsonAdapter).getAddresses();
  }

  public boolean isSubscriber(final String streamId, final Address address) throws IOException {
    return isSubscriber(streamId, address.toString());
  }

  public boolean isSubscriber(final String streamId, final String ethAddress) throws IOException {
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

  /** You might have to close {@code response} if {@code assertSuccessful()} fails. */
  static void assertSuccessful(Response response) throws IOException {
    if (!response.isSuccessful()) {
      String action = response.request().method() + " " + response.request().url().toString();
      final int httpStatusCode = response.code();
      switch (httpStatusCode) {
        case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
          throw new AuthenticationException(action);
        case HttpURLConnection.HTTP_PAYMENT_REQUIRED: // 402
        default: // fallthrough
          final String body = response.body().string();
          final String message =
              String.format("%s failed with HTTP status %d:%s", action, httpStatusCode, body);
          throw new RuntimeException(message);
        case HttpURLConnection.HTTP_FORBIDDEN: // 403
          throw new PermissionDeniedException(action);
        case HttpURLConnection.HTTP_NOT_FOUND: // 404
          throw new ResourceNotFoundException(action);
      }
    }
  }
}
