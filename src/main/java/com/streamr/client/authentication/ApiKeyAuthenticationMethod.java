package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.StreamrClientOptions;
import com.streamr.client.utils.HttpUtils;

import java.io.IOException;

public class ApiKeyAuthenticationMethod extends AuthenticationMethod {

    private JsonAdapter<ApiKeyLoginRequest> adapter;

    public ApiKeyAuthenticationMethod(StreamrClientOptions options) {
        super(options);
        this.adapter = HttpUtils.MOSHI.adapter(ApiKeyLoginRequest.class);
    }

    @Override
    protected LoginResponse login() throws IOException {
        return parse(post("/login/apikey", adapter.toJson(new ApiKeyLoginRequest(options.getApiKey()))));
    }

    static class ApiKeyLoginRequest {
        String apiKey;

        ApiKeyLoginRequest(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
