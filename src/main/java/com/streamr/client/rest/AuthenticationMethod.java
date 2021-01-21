package com.streamr.client.rest;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.protocol.message_layer.Json;
import java.io.IOException;
import java.util.Date;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

public abstract class AuthenticationMethod {

  private JsonAdapter<LoginResponse> responseAdapter;

  public AuthenticationMethod() {
    this.responseAdapter = Json.MOSHI.adapter(LoginResponse.class);
  }

  /**
   * Uses the credentials represented by this class to login and obtain a new, valid sessionToken.
   */
  public String newSessionToken(String restApiUrl) {
    try {
      LoginResponse loginResponse = login(restApiUrl);
      return loginResponse.getSessionToken();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Should call the login endpoint(s) with appropriate credentials to get a LoginResponse. You can
   * use the post(endpoint, requestBody) utility function to do this.
   */
  protected abstract LoginResponse login(String restApiUrl) throws IOException;

  protected Response post(String endpoint, String requestBody) throws IOException {
    OkHttpClient client = new OkHttpClient();

    Request request =
        new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(requestBody, Json.jsonType))
            .build();

    // Execute the request and retrieve the response.
    Response response = client.newCall(request).execute();
    StreamrRESTClient.assertSuccessful(response);
    return response;
  }

  protected LoginResponse parse(BufferedSource json) throws IOException {
    // Deserialize HTTP response to concrete type.
    return responseAdapter.fromJson(json);
  }

  public static class LoginResponse {
    private String token;
    private Date expires;

    public LoginResponse(String token, Date expires) {
      this.token = token;
      this.expires = expires;
    }

    public String getSessionToken() {
      return token;
    }

    public Date getExpiration() {
      return expires;
    }
  }
}
