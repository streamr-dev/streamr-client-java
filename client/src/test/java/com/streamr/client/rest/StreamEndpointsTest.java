package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.StreamrClient;
import com.streamr.client.crypto.Keys;
import com.streamr.client.protocol.rest.FieldConfig;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.protocol.rest.StreamConfig;
import com.streamr.client.protocol.rest.StreamPart;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingStreamrClient;
import com.streamr.client.testing.TestingStreams;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class StreamEndpointsTest {
  private final BigInteger privateKey = TestingKeys.generatePrivateKey();
  private StreamrClient client;

  @BeforeEach
  void setup() {
    client = TestingStreamrClient.createClientWithPrivateKey(privateKey);
  }

  @AfterEach
  void cleanup() {
    client.disconnect();
  }

  @Test
  void createStreamThenGetStream() throws IOException {
    FieldConfig fieldFoo = new FieldConfig("foo", FieldConfig.Type.NUMBER);
    FieldConfig fieldBar = new FieldConfig("bar", FieldConfig.Type.STRING);
    StreamConfig config = new StreamConfig(fieldFoo, fieldBar);
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .withConfig(config)
            .createStream();
    Stream createResult = client.createStream(proto);
    assertNotNull(createResult.getId());
    assertEquals(proto.getName(), createResult.getName());
    assertEquals(proto.getDescription(), createResult.getDescription());
    assertEquals(proto.getConfig(), createResult.getConfig());
    assertEquals(1, createResult.getPartitions());

    Stream getResult = client.getStream(createResult.getId());
    assertEquals(createResult.getId(), getResult.getId());
    assertEquals(createResult.getName(), getResult.getName());
    assertEquals(createResult.getDescription(), getResult.getDescription());
    assertEquals(createResult.getConfig(), getResult.getConfig());
    assertEquals(createResult.getPartitions(), getResult.getPartitions());
    assertFalse(getResult.requiresSignedData());
  }

  @Test
  void createStreamThenGetStreamSettingRequireSignedData() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .withRequireSignedData(true)
            .withConfig(new StreamConfig())
            .createStream();
    Stream createResult = client.createStream(proto);
    assertTrue(createResult.requiresSignedData());
    Stream getResult = client.getStream(createResult.getId());
    assertTrue(getResult.requiresSignedData());
    ;
  }

  @Test
  void createStreamThenGetStreamByName() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream createResult = client.createStream(proto);
    assertNotNull(createResult.getId());
    assertEquals(proto.getName(), createResult.getName());

    Stream getResult = client.getStreamByName(proto.getName());
    assertEquals(createResult.getId(), getResult.getId());
    assertEquals(createResult.getName(), getResult.getName());
  }

  @Test
  void getStreamByNameThrowsResourceNotFoundExceptionIfNoSuchStreamIsFound() {
    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> {
          client.getStreamByName("non-existent for sure " + System.currentTimeMillis());
        });
  }

  @Test
  void getStreamByNameThrowsAmbiguousResultsExceptionIfMultipleMatchingStreamsAreFound()
      throws IOException {
    // Create 2 streams with same name
    String name = TestingStreams.generateName(StreamEndpointsTest.class);
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(name)
            .withDescription("This stream was created from an integration test")
            .createStream();
    client.createStream(proto);
    Stream proto2 =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(name)
            .withDescription("This stream was created from an integration test")
            .createStream();
    client.createStream(proto2);
    assertThrows(
        AmbiguousResultsException.class,
        () -> {
          client.getStreamByName(name);
        });
  }

  @Test
  void createStreamThrowsAuthenticationExceptionIfTheClientIsUnauthenticated() {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    StreamrClient unauthenticatedClient = TestingStreamrClient.createUnauthenticatedClient();
    try {
      assertThrows(
          AuthenticationException.class,
          () -> {
            unauthenticatedClient.createStream(proto);
          });
    } finally {
      unauthenticatedClient.disconnect();
    }
  }

  @Test
  void getStreamThrowsStreamNotFoundExceptionForNonExistentStreams() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          client.getStream("non-existent");
        });
  }

  @Test
  void getStreamThrowsPermissionDeniedExceptionForStreamsWhichTheUserCanNotAccess()
      throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    StreamrClient unauthenticatedClient = TestingStreamrClient.createUnauthenticatedClient();
    Stream createResult = client.createStream(proto);
    assertNotNull(createResult.getId());
    try {
      assertThrows(
          PermissionDeniedException.class,
          () -> {
            unauthenticatedClient.getStream(createResult.getId());
          });
    } finally {
      unauthenticatedClient.disconnect();
    }
  }

  @Test
  void getUserInfo() throws IOException {
    UserInfo info = client.getUserInfo();
    assertEquals("Anonymous User", info.getName());
    assertEquals(Keys.privateKeyToAddressWithPrefix(privateKey), info.getUsername());
  }

  @Test
  void getPublishers() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream createdResult = client.createStream(proto);
    List<String> publishers = client.getPublishers(createdResult.getId());
    List<String> expected = new ArrayList<>();
    expected.add(client.getPublisherId().toString());
    assertEquals(expected, publishers);
  }

  @Test
  void isPublisher() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream createdResult = client.createStream(proto);
    boolean isValid1 = client.isPublisher(createdResult.getId(), client.getPublisherId());
    boolean isValid2 = client.isPublisher(createdResult.getId(), "wrong-address");
    assertTrue(isValid1);
    assertFalse(isValid2);
  }

  @Test
  void getSubscribers() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream createdResult = client.createStream(proto);
    List<String> subscribers = client.getSubscribers(createdResult.getId());
    List<String> expected = new ArrayList<>();
    expected.add(client.getPublisherId().toString());
    assertEquals(expected, subscribers);
  }

  @Test
  void isSubscriber() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream createdResult = client.createStream(proto);
    boolean isValid1 =
        client.isSubscriber(createdResult.getId(), client.getPublisherId().toString());
    boolean isValid2 = client.isSubscriber(createdResult.getId(), "wrong-address");
    assertTrue(isValid1);
    assertFalse(isValid2);
  }

  @Test
  void addStreamToStorageNode() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    String streamId = client.createStream(proto).getId();
    StorageNode storageNode = new StorageNode(TestingAddresses.createRandom());
    client.addStreamToStorageNode(streamId, storageNode);
    List<StorageNode> storageNodes = client.getStorageNodes(streamId);
    assertEquals(1, storageNodes.size());
    assertEquals(storageNode.getAddress(), storageNodes.get(0).getAddress());
  }

  @Test
  void removeStreamFromStorageNode() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    String streamId = client.createStream(proto).getId();
    StorageNode storageNode = new StorageNode(TestingAddresses.createRandom());
    client.addStreamToStorageNode(streamId, storageNode);
    client.removeStreamFromStorageNode(streamId, storageNode);
    List<StorageNode> storageNodes = client.getStorageNodes(streamId);
    assertEquals(0, storageNodes.size());
  }

  @Test
  void getStreamPartsByStorageNode() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamEndpointsTest.class))
            .withName(TestingStreams.generateName(StreamEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .withPartitions(2)
            .createStream();
    String streamId = client.createStream(proto).getId();
    StorageNode storageNode = new StorageNode(TestingAddresses.createRandom());
    client.addStreamToStorageNode(streamId, storageNode);
    List<StreamPart> streamParts = client.getStreamPartsByStorageNode(storageNode);
    assertEquals(2, streamParts.size());
    assertEquals(streamId, streamParts.get(0).getStreamId());
    assertEquals(0, streamParts.get(0).getStreamPartition());
    assertEquals(streamId, streamParts.get(1).getStreamId());
    assertEquals(1, streamParts.get(1).getStreamPartition());
  }

  @Test
  void notSameTokenUsedAfterLogout() throws IOException {
    client.getUserInfo(); // fetches sessionToken1 and requests endpoint
    String sessionToken1 = client.getSessionToken();
    client.logout();
    client.getUserInfo(); // requests with sessionToken1, receives 401, fetches sessionToken2 and
    // requests endpoint
    String sessionToken2 = client.getSessionToken();
    assertNotEquals(sessionToken1, sessionToken2);
  }

  @Test
  void throwsIfLogoutWhenAlreadyLoggedOut() throws IOException {
    client.logout();
    assertThrows(
        AuthenticationException.class,
        () -> {
          client.logout(); // does not retry with a new session token after receiving 401
        });
  }
}
