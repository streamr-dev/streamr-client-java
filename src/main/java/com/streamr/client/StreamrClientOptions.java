package com.streamr.client;

import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.exceptions.InvalidOptionsException;

public class StreamrClientOptions {

    private AuthenticationMethod authenticationMethod = null;
    private SigningOptions signingOptions = SigningOptions.getDefault();
    private boolean publishSignedMsgs = false;
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
        if (this.signingOptions.getPublishSigned().equals("always")) {
            if (authenticationMethod instanceof EthereumAuthenticationMethod) {
                this.publishSignedMsgs = true;
            } else {
                throw new InvalidOptionsException("Signing option 'always' requires a ChallengeAuthenticationMethod as" +
                        "AuthenticationMethod.(Need a private key to be able to sign).");
            }
        } else if (this.signingOptions.getPublishSigned().equals("auto")) {
            this.publishSignedMsgs = authenticationMethod instanceof EthereumAuthenticationMethod;
        }
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

    public boolean publishSignedMsgs() {
        return publishSignedMsgs;
    }

    public String getVerifySignaturesOption() {
        return signingOptions.getVerifySignatures();
    }
}
