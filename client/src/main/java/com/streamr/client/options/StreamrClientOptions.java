package com.streamr.client.options;

import com.streamr.client.ws.WebsocketUrl;

public class StreamrClientOptions {
  private SigningOptions signingOptions =
      new SigningOptions(SigningOptions.SignatureVerificationPolicy.AUTO);
  private WebsocketUrl websocketApiUrl = new WebsocketUrl();
  private long connectionTimeoutMillis = 10 * 1000L;
  private long reconnectRetryInterval = 10 * 1000L;
  private int propagationTimeout = 5000;
  private int resendTimeout = 5000;
  private boolean skipGapsOnFullQueue = true;

  public StreamrClientOptions() {}

  public StreamrClientOptions(SigningOptions signingOptions, String websocketApiUrl) {
    this.signingOptions = signingOptions;
    this.websocketApiUrl = new WebsocketUrl(websocketApiUrl);
  }

  public StreamrClientOptions(
      SigningOptions signingOptions,
      String websocketApiUrl,
      int propagationTimeout,
      int resendTimeout,
      boolean skipGapsOnFullQueue) {
    this(signingOptions, websocketApiUrl);
    this.propagationTimeout = propagationTimeout;
    this.resendTimeout = resendTimeout;
    this.skipGapsOnFullQueue = skipGapsOnFullQueue;
  }

  public String getWebsocketApiUrl() {
    return websocketApiUrl.toString();
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

  public int getPropagationTimeout() {
    return propagationTimeout;
  }

  public int getResendTimeout() {
    return resendTimeout;
  }

  public boolean getSkipGapsOnFullQueue() {
    return skipGapsOnFullQueue;
  }
}
