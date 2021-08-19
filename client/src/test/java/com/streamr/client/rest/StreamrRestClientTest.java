package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.crypto.Keys;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingMeta;
import com.streamr.client.testing.TestingStreamrClient;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class StreamrRestClientTest {
  @Test
  void newSessionTokenFetchesNewSessionTokenBySigningChallenge() throws IOException {
    final BigInteger privateKey = TestingKeys.generatePrivateKey();
    final StreamrRestClient auth = createClient(privateKey);
    final LoginResponse loginResponse = auth.login(privateKey);
    assertNotNull(loginResponse.getToken());
  }

  @Test
  void createStream_withoutStreamId() throws IOException {
    Stream proto = new Stream.Builder().createStream();
    Stream actual = createClient(TestingKeys.generatePrivateKey()).createStream(proto);
    assertNotNull(actual.getId());
  }

  @Test
  void createStream_fullStreamId() throws IOException {
    BigInteger privateKey = TestingKeys.generatePrivateKey();
    String path = "/foobar-" + System.currentTimeMillis();
    String address = Keys.privateKeyToAddressWithPrefix(privateKey);
    Stream proto = new Stream.Builder().withId(address + path).createStream();
    Stream actual = createClient(privateKey).createStream(proto);
    assertEquals(address + path, actual.getId());
  }

  @Test
  void createStream_pathStreamId() throws IOException {
    BigInteger privateKey = TestingKeys.generatePrivateKey();
    String address = Keys.privateKeyToAddressWithPrefix(privateKey);
    String path = "/foobar-" + System.currentTimeMillis();
    Stream proto = new Stream.Builder().withId(path).createStream();
    Stream actual = createClient(privateKey).createStream(proto);
    assertEquals(address.toLowerCase() + path, actual.getId());
  }

  @Test
  void getStreamByName_pathStreamId_noAuthentication() {
    assertThrows(
        AuthenticationException.class,
        () -> {
          Stream proto =
              new Stream.Builder().withId("/foobar-" + System.currentTimeMillis()).createStream();
          TestingStreamrClient.createUnauthenticatedClient().createStream(proto);
        });
  }

  private StreamrRestClient createClient(BigInteger privateKey) {
    return new StreamrRestClient.Builder()
        .withRestApiUrl(TestingMeta.REST_URL)
        .withPrivateKey(privateKey)
        .createStreamrRestClient();
  }
}
