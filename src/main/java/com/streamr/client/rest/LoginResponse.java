package com.streamr.client.rest;

import java.util.Date;
import java.util.Objects;

public final class LoginResponse {
  private final String token;
  private final Date expires;

  public LoginResponse(final String token, final Date expires) {
    this.token = token;
    this.expires = expires;
  }

  /** Get session token. */
  public String getToken() {
    return token;
  }

  /** Get expiration date for session token. */
  public Date getExpiration() {
    return expires;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final LoginResponse that = (LoginResponse) obj;
    return Objects.equals(token, that.token) && Objects.equals(expires, that.expires);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, expires);
  }

  @Override
  public String toString() {
    return String.format("LoginResponse{token='%s', expires=%s}", token, expires);
  }
}
