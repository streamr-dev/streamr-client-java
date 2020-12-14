package com.streamr.client.authentication;

import com.squareup.moshi.JsonAdapter;
import com.streamr.client.utils.HttpUtils;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

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
        Response response = null;
        ResponseBody body = null;
        BufferedSource source = null;
        try {
            response = post(restApiUrl + "/login/apikey", adapter.toJson(new ApiKeyLoginRequest(apiKey)));
            body = response.body();
            source = body.source();
            LoginResponse loginResponse = parse(source);
            return loginResponse;
        } finally {
            if (source != null) {
                source.close();
            }
            if (body != null) {
                body.close();
            }
            if (response != null) {
                response.close();
            }
        }
    }

    static class ApiKeyLoginRequest {
        String apiKey;

        ApiKeyLoginRequest(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
