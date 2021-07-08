package com.streamr.client.options;

import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.exceptions.InvalidOptionsException;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.Address;

public class StreamrClientOptions {

    private AuthenticationMethod authenticationMethod = null;
    private SigningOptions signingOptions = SigningOptions.getDefault();
    private EncryptionOptions encryptionOptions = EncryptionOptions.getDefault();
    private boolean publishSignedMsgs = false;
    private String websocketApiUrl = "wss://www.streamr.com/api/v1/ws" +
            "?controlLayerVersion=" + ControlMessage.LATEST_VERSION +
            "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
    private String restApiUrl = "https://www.streamr.com/api/v1";

    private String mainnetRpcUrl = "https://mainnet.infura.io/v3/140f8dc53a2141e4b645a4db9fc4cebb";
    private String sidechainRpcUrl = "https://rpc.xdaichain.com/";
    private Address storageNodeAddress = new Address("0x31546eEA76F2B2b3C5cC06B1c93601dc35c9D916");
    private String storageNodeUrl = "https://corea1.streamr.network:8001";

    private long connectionTimeoutMillis = 60 * 1000;
    private long reconnectRetryInterval = 10 * 1000;
    private int propagationTimeout = 5000;
    private int resendTimeout = 5000;
    private boolean skipGapsOnFullQueue = true;

    public StreamrClientOptions() {}

    public StreamrClientOptions(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
        this.publishSignedMsgs = authenticationMethod instanceof EthereumAuthenticationMethod;
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
                                EncryptionOptions encryptionOptions, String websocketApiUrl, String restApiUrl) {
        this(authenticationMethod, signingOptions);
        this.encryptionOptions = encryptionOptions;
        this.websocketApiUrl = addMissingQueryString(websocketApiUrl);
        this.restApiUrl = restApiUrl;
    }

    public StreamrClientOptions(AuthenticationMethod authenticationMethod, SigningOptions signingOptions,
                                EncryptionOptions encryptionOptions, String websocketApiUrl, String restApiUrl,
                                int propagationTimeout, int resendTimeout, boolean skipGapsOnFullQueue) {
        this(authenticationMethod, signingOptions, encryptionOptions, websocketApiUrl, restApiUrl);
        this.propagationTimeout = propagationTimeout;
        this.resendTimeout = resendTimeout;
        this.skipGapsOnFullQueue = skipGapsOnFullQueue;
    }

    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public String getWebsocketApiUrl() {
        return websocketApiUrl;
    }

    public void setWebsocketApiUrl(String websocketApiUrl) {
        this.websocketApiUrl = addMissingQueryString(websocketApiUrl);
    }

    public String getRestApiUrl() {
        return restApiUrl;
    }

    public String getMainnetRpcUrl() {
        return mainnetRpcUrl;
    }

    public void setMainnetRpcUrl(String mainnetRpcUrl) {
        this.mainnetRpcUrl = mainnetRpcUrl;
    }

    public String getSidechainRpcUrl() {
        return sidechainRpcUrl;
    }

    public void setSidechainRpcUrl(String sidechainRpcUrl) {
        this.sidechainRpcUrl = sidechainRpcUrl;
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

    public long getReconnectRetryInterval() {
        return reconnectRetryInterval;
    }

    public void setReconnectRetryInterval(long reconnectRetryInterval) {
        this.reconnectRetryInterval = reconnectRetryInterval;
    }

    public boolean getPublishSignedMsgs() {
        return publishSignedMsgs;
    }

    public SigningOptions getSigningOptions() {
        return signingOptions;
    }

    public EncryptionOptions getEncryptionOptions() {
        return encryptionOptions;
    }

    public int getPropagationTimeout() {
        return propagationTimeout;
    }

    public int getResendTimeout() {
        return resendTimeout;
    }

    public boolean getSkipGapsOnFullQueue() {
        return skipGapsOnFullQueue;
    }

    public void setSkipGapsOnFullQueue(boolean skipGapsOnFullQueue) {
        this.skipGapsOnFullQueue = skipGapsOnFullQueue;
    }

    private String addMissingQueryString(String url) {
        String[] parts = url.split("\\?");
        if (parts.length == 1) { // no query string
            return url + "?controlLayerVersion=" + ControlMessage.LATEST_VERSION +
                    "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
        } else {
            String[] params = parts[1].split("&");
            boolean missingControlLayer = true;
            boolean missingMessageLayer = true;
            for (String p: params) {
                if (p.startsWith("controlLayerVersion=")) {
                    missingControlLayer = false;
                } else if (p.startsWith("messageLayerVersion=")) {
                    missingMessageLayer = false;
                }
            }
            String result = url;
            if (missingControlLayer) {
                result += "&controlLayerVersion=" + ControlMessage.LATEST_VERSION;
            }
            if (missingMessageLayer) {
                result += "&messageLayerVersion=" + StreamMessage.LATEST_VERSION;
            }
            return result;
        }
    }


    public void setStorageNodeAddress(Address storageNodeAddress) {
        this.storageNodeAddress = storageNodeAddress;
    }

    public Address getStorageNodeAddress() {
        return this.storageNodeAddress;
    }

    public void setStorageNodeUrl(String storageNodeUrl) {
        this.storageNodeUrl = storageNodeUrl;
    }

    public String getStorageNodeUrl() {
        return this.storageNodeUrl;
    }

}
