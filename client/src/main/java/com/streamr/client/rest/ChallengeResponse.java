package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

public final class ChallengeResponse {
  private final Challenge challenge;
  private final String signature;
  private final String address;

  public Challenge getChallenge() {
    return challenge;
  }

  public String getSignature() {
    return signature;
  }

  public String getAddress() {
    return address;
  }

  public ChallengeResponse(
      final Challenge challenge, final String signature, final String address) {
    Objects.requireNonNull(challenge);
    this.challenge = challenge;
    Objects.requireNonNull(signature);
    this.signature = signature;
    Objects.requireNonNull(address);
    this.address = address;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final ChallengeResponse that = (ChallengeResponse) obj;
    return Objects.equals(challenge, that.challenge)
        && Objects.equals(signature, that.signature)
        && Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(challenge, signature, address);
  }

  @Override
  public String toString() {
    return String.format(
        "ChallengeResponse{challenge=%s, signature='%s', address='%s'}",
        challenge, signature, address);
  }
}
