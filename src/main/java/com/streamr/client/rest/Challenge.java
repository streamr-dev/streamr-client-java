package com.streamr.client.rest;

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
    this.id = id;
    this.challenge = challenge;
    this.expires = expires;
  }

  @Override
  public String toString() {
    return String.format("Challenge{id='%s', challenge='%s', expires=%s}", id, challenge, expires);
  }
}
