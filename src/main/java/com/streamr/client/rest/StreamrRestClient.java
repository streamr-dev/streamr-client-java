package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.SigningUtil;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
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
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/** This class exposes the RESTful API endpoints. */
public class StreamrRestClient {
  public static final String REST_API_URL = "https://www.streamr.com/api/v1";
  private static final String APPLICATION_JSON = "application/json";
  private final String restApiUrl;
  private final Session session;
  private final BigInteger privateKey;

  public BigInteger getPrivateKey() {
    return privateKey;
  }

  public StreamrRestClient(final String restApiUrl, final BigInteger privateKey) {
    if (restApiUrl != null) {
      this.restApiUrl = restApiUrl;
    } else {
      this.restApiUrl = REST_API_URL;
    }
    this.privateKey = privateKey;
    session = new Session(privateKey, this);
  }

  /*
   * Helper functions
   */

  private Request.Builder addAuthenticationHeader(
      final Request.Builder builder, final boolean newToken) {
    if (!session.isAuthenticated()) {
      return builder;
    } else {
      final String sessionToken =
          newToken ? session.getNewSessionToken() : session.getSessionToken();
      final String authorizationHeader = "Authorization";
      builder.removeHeader(authorizationHeader);
      return builder.addHeader(authorizationHeader, String.format("Bearer %s", sessionToken));
    }
  }

  /** You might have to close {@code response} if {@code assertSuccessful()} fails. */
  private void assertSuccessful(final Response response) throws IOException {
    if (!response.isSuccessful()) {
      final Request request = response.request();
      final String action = String.format("%s %s", request.method(), request.url().toString());
      final int httpStatusCode = response.code();
      switch (httpStatusCode) {
        case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
          throw new AuthenticationException(action);
        case HttpURLConnection.HTTP_FORBIDDEN: // 403
          throw new PermissionDeniedException(action);
        case HttpURLConnection.HTTP_NOT_FOUND: // 404
          throw new ResourceNotFoundException(action);
        case HttpURLConnection.HTTP_PAYMENT_REQUIRED: // 402
        default: // fallthrough
          final String body = response.body().string();
          final String message =
              String.format("%s failed with HTTP status %d:%s", action, httpStatusCode, body);
          throw new RuntimeException(message);
      }
    }
  }

  private <T> T execute(final Request request, final JsonAdapter<T> bodyJsonAdapter)
      throws IOException {
    final OkHttpClient client = new OkHttpClient();

    // Execute the request and retrieve the response.
    final Call call = client.newCall(request);
    final Response response = call.execute();
    try {
      assertSuccessful(response);

      // Deserialize HTTP response to concrete type.
      if (bodyJsonAdapter == null) {
        return null;
      } else {
        final ResponseBody body = response.body();
        BufferedSource source = null;
        if (body != null) {
          source = body.source();
        }
        T result = null;
        if (source != null) {
          result = bodyJsonAdapter.fromJson(source);
        }
        return result;
      }
    } finally {
      response.close(); // closing response will also close source and body
    }
  }

  private <T> T executeWithRetry(
      final Request.Builder builder,
      final JsonAdapter<T> adapter,
      final boolean retryIfSessionExpired)
      throws IOException {
    final Request request = addAuthenticationHeader(builder, false).build();
    try {
      return execute(request, adapter);
    } catch (final AuthenticationException e) {
      if (retryIfSessionExpired) {
        final Request request2 = addAuthenticationHeader(builder, true).build();
        return execute(request2, adapter);
      } else {
        throw e;
      }
    }
  }

  private <T> T getWithRetry(final HttpUrl url, final JsonAdapter<T> adapter) throws IOException {
    final Request.Builder builder = new Request.Builder().url(url);
    return executeWithRetry(builder, adapter, true);
  }

  private <T> T postWithRetry(
      final HttpUrl url, final String requestBody, final JsonAdapter<T> adapter)
      throws IOException {
    return postWithRetry(url, requestBody, adapter, true);
  }

  private <T> T postWithRetry(
      final HttpUrl url,
      final String requestBody,
      final JsonAdapter<T> adapter,
      final boolean retryIfSessionExpired)
      throws IOException {
    final MediaType contentTypeJson = MediaType.parse(APPLICATION_JSON);
    final RequestBody content = RequestBody.create(requestBody, contentTypeJson);
    final Request.Builder builder = new Request.Builder().url(url).post(content);
    return executeWithRetry(builder, adapter, retryIfSessionExpired);
  }

  /*
   * Stream endpoints
   */

  public Stream getStream(final String streamId) throws IOException, ResourceNotFoundException {
    if (streamId == null) {
      throw new IllegalArgumentException("streamId cannot be null!");
    }

    final HttpUrl url = getEndpointUrl("streams", streamId);
    final JsonAdapter<Stream> streamJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Stream.class);
    return getWithRetry(url, streamJsonAdapter);
  }

  public Stream getStreamByName(final String name) throws IOException, AmbiguousResultsException {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Stream name must be specified!");
    }

    final HttpUrl url =
        getEndpointUrl("streams").newBuilder().setQueryParameter("name", name).build();

    final ParameterizedType pt = Types.newParameterizedType(List.class, Stream.class);
    final JsonAdapter<List<Stream>> streamListJsonAdapter =
        Json.newMoshiBuilder().build().adapter(pt);
    final List<Stream> matches = getWithRetry(url, streamListJsonAdapter);
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
    final HttpUrl url = getEndpointUrl("streams");
    final JsonAdapter<Stream> streamJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Stream.class);
    return postWithRetry(url, streamJsonAdapter.toJson(stream), streamJsonAdapter);
  }

  public Permission grant(
      final Stream stream, final Permission.Operation operation, final String user)
      throws IOException {
    if (stream == null || operation == null || user == null) {
      throw new IllegalArgumentException("Must give all of stream, operation, and user!");
    }

    final Permission permission = new Permission(operation, user);

    final HttpUrl url = getEndpointUrl("streams", stream.getId(), "permissions");
    final JsonAdapter<Permission> permissionJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Permission.class);
    return postWithRetry(url, permissionJsonAdapter.toJson(permission), permissionJsonAdapter);
  }

  public Permission grantPublic(final Stream stream, final Permission.Operation operation)
      throws IOException {
    if (stream == null || operation == null) {
      throw new IllegalArgumentException("Must give stream and operation!");
    }

    final Permission permission = new Permission(operation);

    final HttpUrl url = getEndpointUrl("streams", stream.getId(), "permissions");
    final JsonAdapter<Permission> permissionJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Permission.class);
    return postWithRetry(url, permissionJsonAdapter.toJson(permission), permissionJsonAdapter);
  }

  public UserInfo getUserInfo() throws IOException {
    final HttpUrl url = getEndpointUrl("users", "me");
    final JsonAdapter<UserInfo> userInfoJsonAdapter =
        Json.newMoshiBuilder().build().adapter(UserInfo.class);
    return getWithRetry(url, userInfoJsonAdapter);
  }

  public List<String> getPublishers(final String streamId) throws IOException {
    final HttpUrl url = getEndpointUrl("streams", streamId, "publishers");
    final JsonAdapter<Publishers> publishersJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Publishers.class);
    return getWithRetry(url, publishersJsonAdapter).getAddresses();
  }

  public boolean isPublisher(final String streamId, final Address address) throws IOException {
    return isPublisher(streamId, address.toString());
  }

  public boolean isPublisher(final String streamId, final String ethAddress) throws IOException {
    final HttpUrl url = getEndpointUrl("streams", streamId, "publisher", ethAddress);
    try {
      getWithRetry(url, null);
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  public List<String> getSubscribers(final String streamId) throws IOException {
    final HttpUrl url = getEndpointUrl("streams", streamId, "subscribers");
    final JsonAdapter<Subscribers> subscribersJsonAdapter =
        Json.newMoshiBuilder().build().adapter(Subscribers.class);
    return getWithRetry(url, subscribersJsonAdapter).getAddresses();
  }

  public boolean isSubscriber(final String streamId, final Address address) throws IOException {
    return isSubscriber(streamId, address.toString());
  }

  public boolean isSubscriber(final String streamId, final String ethAddress) throws IOException {
    final HttpUrl url = getEndpointUrl("streams", streamId, "subscriber", ethAddress);
    try {
      getWithRetry(url, null);
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  public void logout() throws IOException {
    final HttpUrl url = getEndpointUrl("logout");
    postWithRetry(url, "", null, false);
  }

  private HttpUrl getEndpointUrl(final String... pathSegments) {
    HttpUrl.Builder builder = HttpUrl.parse(restApiUrl).newBuilder();
    for (String segment : pathSegments) {
      builder = builder.addPathSegment(segment);
    }
    return builder.build();
  }

  public String getSessionToken() {
    return session.getSessionToken();
  }

  private String toAddress(final BigInteger privateKey) {
    final ECKeyPair account = ECKeyPair.create(privateKey);
    final String addr = Keys.getAddress(account.getPublicKey());
    return Numeric.prependHexPrefix(addr);
  }

  public LoginResponse login(final BigInteger privateKey) throws IOException {
    final Challenge challenge = getChallenge(privateKey);
    final String signature = SigningUtil.sign(privateKey, challenge.getChallenge());
    final String address = toAddress(privateKey);
    final ChallengeResponse response = new ChallengeResponse(challenge, signature, address);

    final HttpUrl url = getEndpointUrl("login", "response");
    final MediaType mediaType = MediaType.parse(APPLICATION_JSON);
    final JsonAdapter<ChallengeResponse> challengeResponseAdapter =
        Json.newMoshiBuilder().build().adapter(ChallengeResponse.class);
    final RequestBody requestBody =
        RequestBody.create(challengeResponseAdapter.toJson(response), mediaType);
    final Request request = new Request.Builder().url(url).post(requestBody).build();
    final JsonAdapter<LoginResponse> loginResponseAdapter =
        Json.newMoshiBuilder().build().adapter(LoginResponse.class);
    final LoginResponse result = execute(request, loginResponseAdapter);
    return result;
  }

  private Challenge getChallenge(final BigInteger privateKey) throws IOException {
    final String address = toAddress(privateKey);
    final HttpUrl url = getEndpointUrl("login", "challenge", address);
    final MediaType mediaType = MediaType.parse(APPLICATION_JSON);
    final RequestBody requestBody = RequestBody.create("", mediaType);
    final Request request = new Request.Builder().url(url).post(requestBody).build();
    final JsonAdapter<Challenge> challengeAdapter =
        Json.newMoshiBuilder().build().adapter(Challenge.class);
    final Challenge result = execute(request, challengeAdapter);
    return result;
  }
}
