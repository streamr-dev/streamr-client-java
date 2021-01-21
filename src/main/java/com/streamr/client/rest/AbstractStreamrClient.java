package com.streamr.client.rest;

import com.squareup.moshi.Moshi;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.message_layer.Json;
import com.streamr.client.utils.Address;
import java.io.IOException;
import java.util.List;

/**
 * Provides the barebones of a StreamrClient, including holding the config, providing JSON
 * serializers etc.
 */
public abstract class AbstractStreamrClient {
  protected static final Moshi MOSHI = Json.MOSHI;

  protected final StreamrClientOptions options;

  protected final Session session;

  public AbstractStreamrClient(StreamrClientOptions options) {
    this.options = options;

    // Create Session object based on what kind of authentication method is provided in options
    session = new Session(options.getRestApiUrl(), options.getAuthenticationMethod());
  }

  public StreamrClientOptions getOptions() {
    return options;
  }

  public String getSessionToken() {
    return session.getSessionToken();
  }

  // TODO: These methods below are part of StreamrClient interface

  public abstract Stream createStream(final Stream stream) throws IOException;

  public abstract Permission grant(
      final Stream stream, final Permission.Operation operation, final String user)
      throws IOException;

  public abstract Permission grantPublic(final Stream stream, final Permission.Operation operation)
      throws IOException;

  public abstract UserInfo getUserInfo() throws IOException;

  public abstract List<String> getPublishers(final String streamId) throws IOException;

  public abstract boolean isPublisher(final String streamId, final Address address)
      throws IOException;

  public abstract boolean isPublisher(final String streamId, final String ethAddress)
      throws IOException;

  public abstract List<String> getSubscribers(final String streamId) throws IOException;

  public abstract boolean isSubscriber(final String streamId, final Address address)
      throws IOException;

  public abstract boolean isSubscriber(final String streamId, final String ethAddress)
      throws IOException;

  public abstract void logout() throws IOException;
}
