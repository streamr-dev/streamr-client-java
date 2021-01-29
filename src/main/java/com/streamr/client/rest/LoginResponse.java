package com.streamr.client.rest;

import java.util.Date;

public final class LoginResponse {
  private final String token;
  private final Date expires;

  public LoginResponse(final String token, final Date expires) {
    this.token = token;
    this.expires = expires;
  }

  public String getToken() {
    return token;
  }

  public Date getExpiration() {
    return expires;
  }

  @Override
  public String toString() {
    return String.format("LoginResponse{token='%s', expires=%s}", token, expires);
  }
}
