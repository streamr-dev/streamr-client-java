package com.streamr.client.options;

import com.streamr.client.exceptions.InvalidOptionsException;
import com.streamr.client.rest.AuthenticationMethod;
import com.streamr.client.rest.EthereumAuthenticationMethod;
import com.streamr.client.ws.WebsocketUrl;

public class StreamrClientOptions {

  private AuthenticationMethod authenticationMethod = null;
  private SigningOptions signingOptions = SigningOptions.getDefault();
  private EncryptionOptions encryptionOptions = EncryptionOptions.getDefault();
  private boolean publishSignedMsgs = false;
  private WebsocketUrl websocketApiUrl = new WebsocketUrl();
  private String restApiUrl = "https://www.streamr.com/api/v1";

  private String mainnetRpcUrl = "http://localhost:8545";
  private String sidechainRpcUrl = "http://localhost:8546";
  private String dataUnionSidechainFactoryAddress = "0x4081B7e107E59af8E82756F96C751174590989FE";
  private String dataUnionMainnetFactoryAddress = "0x5E959e5d5F3813bE5c6CeA996a286F734cc9593b";

  private long connectionTimeoutMillis = 10 * 1000;
  private long reconnectRetryInterval = 10 * 1000;
  private int propagationTimeout = 5000;
  private int resendTimeout = 5000;
  private boolean skipGapsOnFullQueue = true;

  public StreamrClientOptions() {}

  public StreamrClientOptions(AuthenticationMethod authenticationMethod) {
    this.authenticationMethod = authenticationMethod;
    this.publishSignedMsgs = authenticationMethod instanceof EthereumAuthenticationMethod;
  }

  public StreamrClientOptions(
      AuthenticationMethod authenticationMethod, SigningOptions signingOptions) {
    this.authenticationMethod = authenticationMethod;
    this.signingOptions = signingOptions;
    if (this.signingOptions.getPublishSigned()
        == SigningOptions.SignatureComputationPolicy.ALWAYS) {
      if (authenticationMethod instanceof EthereumAuthenticationMethod) {
        this.publishSignedMsgs = true;
      } else {
        throw new InvalidOptionsException(
            "SigningOptions.SignatureComputationPolicy.ALWAYS requires an EthereumAuthenticationMethod as"
                + "AuthenticationMethod.(Need a private key to be able to sign).");
      }
    } else if (this.signingOptions.getPublishSigned()
        == SigningOptions.SignatureComputationPolicy.AUTO) {
      this.publishSignedMsgs = authenticationMethod instanceof EthereumAuthenticationMethod;
    }
  }

  public StreamrClientOptions(
      AuthenticationMethod authenticationMethod,
      SigningOptions signingOptions,
      EncryptionOptions encryptionOptions,
      String websocketApiUrl,
      String restApiUrl) {
    this(authenticationMethod, signingOptions);
    this.encryptionOptions = encryptionOptions;
    this.websocketApiUrl = new WebsocketUrl(websocketApiUrl);
    this.restApiUrl = restApiUrl;
  }

  public StreamrClientOptions(
      AuthenticationMethod authenticationMethod,
      SigningOptions signingOptions,
      EncryptionOptions encryptionOptions,
      String websocketApiUrl,
      String restApiUrl,
      int propagationTimeout,
      int resendTimeout,
      boolean skipGapsOnFullQueue) {
    this(authenticationMethod, signingOptions, encryptionOptions, websocketApiUrl, restApiUrl);
    this.propagationTimeout = propagationTimeout;
    this.resendTimeout = resendTimeout;
    this.skipGapsOnFullQueue = skipGapsOnFullQueue;
  }

  public AuthenticationMethod getAuthenticationMethod() {
    return authenticationMethod;
  }

  public String getWebsocketApiUrl() {
    return websocketApiUrl.toString();
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
