package com.streamr.client;

import com.streamr.client.dataunion.DataUnionClient;
import com.streamr.client.options.ResendOption;
import com.streamr.client.rest.AmbiguousResultsException;
import com.streamr.client.rest.Permission;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.StreamrRestClient;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.Subscription;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.GroupKey;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

  Stream createStream(final Stream stream) throws IOException;

  Stream getStream(final String streamId) throws IOException;

  Stream getStreamByName(final String name) throws IOException, AmbiguousResultsException;

  Permission grant(final Stream stream, final Permission.Operation operation, final String user)
      throws IOException;

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

  String getSessionToken();

  void publish(final Stream stream, final Map<String, Object> payload);

  void publish(final Stream stream, final Map<String, Object> payload, final GroupKey groupKey);

  void publish(final Stream stream, final Map<String, Object> payload, final Date timestamp);

  void publish(
      final Stream stream,
      final Map<String, Object> payload,
      final Date timestamp,
      final GroupKey groupKey);

  void publish(
      final Stream stream,
      final Map<String, Object> payload,
      final Date timestamp,
      final String partitionKey);

  void publish(
      final Stream stream,
      final Map<String, Object> payload,
      final Date timestamp,
      final String partitionKey,
      final GroupKey newGroupKey);

  GroupKey rekey(final Stream stream);

  Subscription subscribe(final Stream stream, final MessageHandler handler);

  Subscription subscribe(
      final Stream stream,
      final int partition,
      final MessageHandler handler,
      final ResendOption resendOption);

  void resend(
      final Stream stream,
      final int partition,
      final MessageHandler handler,
      final ResendOption resendOption);

  void unsubscribe(final Subscription sub);
}
