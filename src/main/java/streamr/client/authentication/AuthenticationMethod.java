package streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import streamr.client.StreamrClientOptions;
import streamr.client.exceptions.AuthenticationException;
import streamr.client.utils.HttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Date;

public abstract class AuthenticationMethod {

    protected final StreamrClientOptions options;
    private JsonAdapter<LoginResponse> responseAdapter;

    public AuthenticationMethod(StreamrClientOptions options) {
        this.options = options;
        this.responseAdapter = HttpUtils.MOSHI.adapter(LoginResponse.class);
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

    protected LoginResponse post(String endpoint, String requestBody) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                        .url(options.getRestApiUrl() + endpoint)
                        .post(RequestBody.create(HttpUtils.jsonType, requestBody))
                        .build();

        // Execute the request and retrieve the response.
        Response response = client.newCall(request).execute();

        // TODO: remove this hack once the API returns 401 on invalid credentials instead of 400
        if (response.code() == 400) {
            throw new AuthenticationException(endpoint);
        }
        HttpUtils.assertSuccessful(response);

        // Deserialize HTTP response to concrete type.
        return responseAdapter.fromJson(response.body().source());
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
