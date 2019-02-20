package com.streamr.client;

import org.apache.commons.codec.binary.Hex;
import org.ethereum.crypto.ECKey;

import java.math.BigInteger;

public class StreamrClientOptions {

    private String apiKey = null;
    private ECKey account = null;
    private String address = null;
    private String websocketApiUrl = "wss://www.streamr.com/api/v1/ws";
    private String restApiUrl = "https://www.streamr.com/api/v1";
    private long connectionTimeoutMillis = 10 * 1000;

    public StreamrClientOptions() {}

    public StreamrClientOptions(String apiKey) {
        this.apiKey = apiKey;
    }

    public StreamrClientOptions(String apiKey, String websocketApiUrl, String restApiUrl) {
        this.apiKey = apiKey;
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
    }

    public StreamrClientOptions(String apiKey, String websocketApiUrl, String restApiUrl, String ethereumPrivateKey) {
        this.apiKey = apiKey;
        this.websocketApiUrl = websocketApiUrl;
        this.restApiUrl = restApiUrl;
        String withoutPrefix = ethereumPrivateKey.startsWith("0x") ? ethereumPrivateKey.substring(2) : ethereumPrivateKey;
        this.account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16));
        this.address = "0x" + Hex.encodeHexString(this.account.getAddress());
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

    public String getApiKey() {
        return apiKey;
    }

    public ECKey getAccount() {
        return account;
    }

    public String getAddress() {
        return address;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }
}
