package streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import streamr.client.StreamrClientOptions;
import streamr.client.utils.HttpUtils;

import java.io.IOException;

public class ApiKeyAuthenticationMethod extends AuthenticationMethod {

    private JsonAdapter<ApiKeyLoginRequest> adapter;

    public ApiKeyAuthenticationMethod(StreamrClientOptions options) {
        super(options);
        this.adapter = HttpUtils.MOSHI.adapter(ApiKeyLoginRequest.class);
    }

    @Override
    protected LoginResponse login() throws IOException {
        return post("/login/apikey", adapter.toJson(new ApiKeyLoginRequest(options.getApiKey())));
    }

    static class ApiKeyLoginRequest {
        String apiKey;

        ApiKeyLoginRequest(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
