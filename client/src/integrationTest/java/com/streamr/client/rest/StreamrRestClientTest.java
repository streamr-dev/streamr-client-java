package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingMeta;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class StreamrRestClientTest {
  @Test
  void newSessionTokenFetchesNewSessionTokenBySigningChallenge() throws IOException {
    final BigInteger privateKey = TestingKeys.generatePrivateKey();
    final StreamrRestClient auth =
        new StreamrRestClient.Builder()
            .withRestApiUrl(TestingMeta.REST_URL)
            .withPrivateKey(privateKey)
            .createStreamrRestClient();
    final LoginResponse loginResponse = auth.login(privateKey);
    assertNotNull(loginResponse.getToken());
  }
}
