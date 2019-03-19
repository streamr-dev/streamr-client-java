package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.StreamrClientOptions;
import com.streamr.client.utils.HttpUtils;

import java.io.IOException;

public class ApiKeyAuthenticationMethod extends AuthenticationMethod {

    private JsonAdapter<ApiKeyLoginRequest> adapter;
    private final String apiKey;

    public ApiKeyAuthenticationMethod(String apiKey) {
        super();
        this.apiKey = apiKey;
        this.adapter = HttpUtils.MOSHI.adapter(ApiKeyLoginRequest.class);
    }

    @Override
    protected LoginResponse login(String restApiUrl) throws IOException {
        return parse(post(restApiUrl + "/login/apikey", adapter.toJson(new ApiKeyLoginRequest(apiKey))));
    }

    static class ApiKeyLoginRequest {
        String apiKey;

        ApiKeyLoginRequest(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
