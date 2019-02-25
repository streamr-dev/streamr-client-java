package com.streamr.client;

import com.streamr.client.authentication.AuthenticationMethod;

public class StreamrClientOptions {

    private AuthenticationMethod authenticationMethod = null;
    private SigningOptions signingOptions = SigningOptions.getDefault();
    private String websocketApiUrl = "wss://www.streamr.com/api/v1/ws";
    private String restApiUrl = "https://www.streamr.com/api/v1";
    private long connectionTimeoutMillis = 10 * 1000;

    public StreamrClientOptions() {}

    public StreamrClientOptions(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions) {
        this.authenticationMethod = authenticationMethod;
        this.signingOptions = signingOptions;
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions, String websocketApiUrl, String restApiUrl) {
        this(authenticationMethod, signingOptions);
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
    }

    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public String getWebsocketApiUrl() {
        return websocketApiUrl;
    }

    public void setWebsocketApiUrl(String websocketApiUrl) {
        this.websocketApiUrl = websocketApiUrl;
    }

    public String getRestApiUrl() {
        return restApiUrl;
    }

    public void setRestApiUrl(String restApiUrl) {
        this.restApiUrl = restApiUrl;
    }

    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
}
