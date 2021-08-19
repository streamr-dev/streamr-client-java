package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.StreamrClient;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingStreamrClient;
import com.streamr.client.testing.TestingStreams;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class PermissionEndpointsTest {
  private StreamrClient grantor;
  private StreamrClient grantee;

  @BeforeEach
  void setup() {
    grantor = TestingStreamrClient.createClientWithPrivateKey(TestingKeys.generatePrivateKey());
    grantee =
        TestingStreamrClient.createClientWithPrivateKey(
            new BigInteger("12beab9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820", 16));
  }

  @AfterEach
  void cleanup() {
    grantor.disconnect();
    grantee.disconnect();
  }

  @Test
  void grant() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(PermissionEndpointsTest.class))
            .withName(TestingStreams.generateName(PermissionEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream stream = grantor.createStream(proto);
    Permission p =
        grantor.grant(stream, Permission.Operation.stream_get, grantee.getPublisherId().toString());
    assertNotNull(p.getId());
    assertEquals(Permission.Operation.stream_get, p.getOperation());
    assertEquals(grantee.getPublisherId().toString(), p.getUser());

    Stream granteeStream = grantee.getStream(stream.getId());
    assertEquals(stream.getId(), granteeStream.getId());
  }

  @Test
  void grantPublic() throws IOException {
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(PermissionEndpointsTest.class))
            .withName(TestingStreams.generateName(PermissionEndpointsTest.class))
            .withDescription("This stream was created from an integration test")
            .createStream();
    Stream stream = grantor.createStream(proto);
    Permission p = grantor.grantPublic(stream, Permission.Operation.stream_get);
    assertNotNull(p.getId());
    assertEquals(Permission.Operation.stream_get, p.getOperation());
    assertTrue(p.getAnonymous());

    Stream granteeStream = grantee.getStream(stream.getId());
    assertEquals(stream.getId(), granteeStream.getId());
  }
}
