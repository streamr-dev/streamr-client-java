package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingMeta;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class StreamrRestClientTest {
  @Test
  void newSessionTokenFetchesNewSessionTokenBySigningChallenge() throws IOException {
    final BigInteger privateKey = TestingKeys.generatePrivateKey();
    final StreamrRestClient auth = new StreamrRestClient(TestingMeta.REST_URL, privateKey);
    final LoginResponse loginResponse = auth.login(privateKey);
    assertNotNull(loginResponse.getToken());
  }
}
