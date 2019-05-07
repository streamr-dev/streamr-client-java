package com.streamr.client.options;

import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.exceptions.InvalidOptionsException;

public class StreamrClientOptions {

    private AuthenticationMethod authenticationMethod = null;
    private SigningOptions signingOptions = SigningOptions.getDefault();
    private boolean publishSignedMsgs = false;
    private String websocketApiUrl = "wss://www.streamr.com/api/v1/ws?controlLayerVersion=1&messageLayerVersion=30";
    private String restApiUrl = "https://www.streamr.com/api/v1";
    private long connectionTimeoutMillis = 10 * 1000;
    private int gapFillTimeout = 5000;
    private int retryResendAfter = 5000;

    public StreamrClientOptions() {}

    public StreamrClientOptions(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions) {
        this.authenticationMethod = authenticationMethod;
        this.signingOptions = signingOptions;
        if (this.signingOptions.getPublishSigned() == SigningOptions.SignatureComputationPolicy.ALWAYS) {
            if (authenticationMethod instanceof EthereumAuthenticationMethod) {
                this.publishSignedMsgs = true;
            } else {
                throw new InvalidOptionsException("SigningOptions.SignatureComputationPolicy.ALWAYS requires an EthereumAuthenticationMethod as" +
                        "AuthenticationMethod.(Need a private key to be able to sign).");
            }
        } else if (this.signingOptions.getPublishSigned() == SigningOptions.SignatureComputationPolicy.AUTO) {
            this.publishSignedMsgs = authenticationMethod instanceof EthereumAuthenticationMethod;
        }
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions,
                                String websocketApiUrl, String restApiUrl) {
        this(authenticationMethod, signingOptions);
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions,
                                String websocketApiUrl, String restApiUrl, int gapFillTimeout, int retryResendAfter) {
        this(authenticationMethod, signingOptions);
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
        this.gapFillTimeout = gapFillTimeout;
        this.retryResendAfter = retryResendAfter;
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

    public boolean getPublishSignedMsgs() {
        return publishSignedMsgs;
    }

    public SigningOptions getSigningOptions() {
        return signingOptions;
    }

    public int getGapFillTimeout() {
        return gapFillTimeout;
    }

    public int getRetryResendAfter() {
        return retryResendAfter;
    }
}
