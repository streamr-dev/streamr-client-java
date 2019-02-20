package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.StreamrClientOptions;
import com.streamr.client.exceptions.AuthenticationException;
import com.streamr.client.utils.HttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

import java.io.IOException;
import java.util.Date;

public abstract class AuthenticationMethod {

    private String restApiUrl;
    private JsonAdapter<LoginResponse> responseAdapter = HttpUtils.MOSHI.adapter(LoginResponse.class);

    public AuthenticationMethod() {
        this.responseAdapter = HttpUtils.MOSHI.adapter(LoginResponse.class);
    }

    public void setRestApiUrl(String restApiUrl) {
        this.restApiUrl = restApiUrl;
    }

    /**
     * Uses the credentials represented by this class to login and obtain a new, valid sessionToken.
     */
    public String newSessionToken() {
        try {
            LoginResponse loginResponse = login();
            return loginResponse.getSessionToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Should call the login endpoint(s) with appropriate credentials to get a LoginResponse.
     * You can use the post(endpoint, requestBody) utility function to do this.
     */
    protected abstract LoginResponse login() throws IOException;

    protected BufferedSource post(String endpoint, String requestBody) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                        .url(restApiUrl + endpoint)
                        .post(RequestBody.create(HttpUtils.jsonType, requestBody))
                        .build();

        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();

        if (response.code() == 401) {
            throw new AuthenticationException(endpoint);
        }
        HttpUtils.assertSuccessful(response);

        return response.body().source();
    }

    protected LoginResponse parse(BufferedSource json) throws IOException {
        // Deserialize HTTP response to concrete type.
        return responseAdapter.fromJson(json);
    }

    static class LoginResponse {
        private String token;
        private Date expires;

        public String getSessionToken() {
            return token;
        }

        public Date getExpiration() {
            return expires;
        }
    }

}
