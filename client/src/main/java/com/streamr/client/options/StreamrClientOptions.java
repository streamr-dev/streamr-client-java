package com.streamr.client.options;

import com.streamr.client.protocol.options.SigningOptions;
import com.streamr.client.ws.WebsocketUrl;

public class StreamrClientOptions {
  private SigningOptions signingOptions = SigningOptions.getDefault();
  private EncryptionOptions encryptionOptions = EncryptionOptions.getDefault();
  private WebsocketUrl websocketApiUrl = new WebsocketUrl();

  private String mainnetRpcUrl = "https://mainnet.infura.io/v3/140f8dc53a2141e4b645a4db9fc4cebb";
  private String sidechainRpcUrl = "https://rpc.xdaichain.com/";
  private String dataUnionSidechainFactoryAddress = "0x1b55587Beea0b5Bc96Bb2ADa56bD692870522e9f";
  private String dataUnionMainnetFactoryAddress = "0x7d55f9981d4E10A193314E001b96f72FCc901e40";
  private long connectionTimeoutMillis = 10 * 1000;
  private long reconnectRetryInterval = 10 * 1000;
  private int propagationTimeout = 5000;
  private int resendTimeout = 5000;
  private boolean skipGapsOnFullQueue = true;

  public StreamrClientOptions() {}

  public StreamrClientOptions(
      SigningOptions signingOptions, EncryptionOptions encryptionOptions, String websocketApiUrl) {
    this.signingOptions = signingOptions;
    this.encryptionOptions = encryptionOptions;
    this.websocketApiUrl = new WebsocketUrl(websocketApiUrl);
  }

  public StreamrClientOptions(
      SigningOptions signingOptions,
      EncryptionOptions encryptionOptions,
      String websocketApiUrl,
      int propagationTimeout,
      int resendTimeout,
      boolean skipGapsOnFullQueue) {
    this(signingOptions, encryptionOptions, websocketApiUrl);
    this.propagationTimeout = propagationTimeout;
    this.resendTimeout = resendTimeout;
    this.skipGapsOnFullQueue = skipGapsOnFullQueue;
  }

  public String getWebsocketApiUrl() {
    return websocketApiUrl.toString();
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

  public String getDataUnionSidechainFactoryAddress() {
    return dataUnionSidechainFactoryAddress;
  }

  public void setDataUnionSidechainFactoryAddress(String dataUnionSidechainFactoryAddress) {
    this.dataUnionSidechainFactoryAddress = dataUnionSidechainFactoryAddress;
  }

  public String getDataUnionMainnetFactoryAddress() {
    return dataUnionMainnetFactoryAddress;
  }

  public void setDataUnionMainnetFactoryAddress(String dataUnionMainnetFactoryAddress) {
    this.dataUnionMainnetFactoryAddress = dataUnionMainnetFactoryAddress;
  }
}
