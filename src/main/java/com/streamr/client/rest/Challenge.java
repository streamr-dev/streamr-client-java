package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;
import java.util.Date;

public final class Challenge {
  private final String id;
  private final String challenge;
  private final Date expires;

  public String getId() {
    return id;
  }

  public String getChallenge() {
    return challenge;
  }

  public Date getExpires() {
    return expires;
  }

  public Challenge(final String id, final String challenge, final Date expires) {
    Objects.requireNonNull(id);
    this.id = id;
    Objects.requireNonNull(challenge);
    this.challenge = challenge;
    Objects.requireNonNull(expires);
    this.expires = expires;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Challenge challenge1 = (Challenge) obj;
    return Objects.equals(id, challenge1.id)
        && Objects.equals(challenge, challenge1.challenge)
        && Objects.equals(expires, challenge1.expires);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, challenge, expires);
  }

  @Override
  public String toString() {
    return String.format("Challenge{id='%s', challenge='%s', expires=%s}", id, challenge, expires);
  }
}
