package com.streamr.client;

import com.streamr.client.dataunion.DataUnionClient;
import com.streamr.client.rest.Permission;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.StreamrRestClient;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.utils.Address;
import java.io.IOException;
import java.util.List;

interface Streamr {
  /** Build unauthenticated Streamr client. */
  static Streamr build() {
    return null;
  }
  /** Build authenticated Streamr client. */
  static Streamr build(final String privateKey) {
    return null;
  }
  /**
   * Build authenticated Streamr client with given {@code StreamrRestClient} and {@code
   * StreamrWebSocketClient} implementations.
   */
  static Streamr build(
      final String privateKey,
      final StreamrRestClient restClient,
      final StreamrClient.StreamrWebSocketClient wsClient) {
    return null;
  }

  Stream getStream(final String streamId) throws IOException;

  Permission grantPublic(final Stream stream, final Permission.Operation operation)
      throws IOException;

  UserInfo getUserInfo() throws IOException;

  List<String> getPublishers(final String streamId) throws IOException;

  boolean isPublisher(final String streamId, final Address address) throws IOException;

  boolean isPublisher(final String streamId, final String ethAddress) throws IOException;

  List<String> getSubscribers(final String streamId) throws IOException;

  boolean isSubscriber(final String streamId, final Address address) throws IOException;

  boolean isSubscriber(final String streamId, final String ethAddress) throws IOException;

  DataUnionClient dataUnionClient(
      final String mainnetAdminPrvKey, final String sidechainAdminPrvKey);

  void logout() throws IOException;
}
