package com.streamr.client.rest;

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
    this.challenge = challenge;
    this.signature = signature;
    this.address = address;
  }

  @Override
  public String toString() {
    return String.format(
        "ChallengeResponse{challenge=%s, signature='%s', address='%s'}",
        challenge, signature, address);
  }
}
