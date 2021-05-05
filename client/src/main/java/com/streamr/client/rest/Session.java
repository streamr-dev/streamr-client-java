package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;
import java.io.IOException;
import java.math.BigInteger;

/** Holds Ethereum private key for getting new sessionTokens, and holds the current sessionToken. */
public final class Session {
  private final BigInteger privateKey;
  private final StreamrRestClient restClient;
  private String sessionToken = null;

  public Session(final BigInteger privateKey, final StreamrRestClient restClient) {
    if (privateKey != null) {
      this.privateKey = privateKey;
    } else {
      this.privateKey = null;
    }
    Objects.requireNonNull(restClient);
    this.restClient = restClient;
  }

  public boolean isAuthenticated() {
    return privateKey != null;
  }

  public String getSessionToken() {
    if (sessionToken == null && isAuthenticated()) {
      try {
        final LoginResponse loginResponse = restClient.login(privateKey);
        sessionToken = loginResponse.getToken();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
    return sessionToken;
  }

  public String getNewSessionToken() {
    sessionToken = null;
    return getSessionToken();
  }
}
