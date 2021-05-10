package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

final class DataUnionJoinRequest {
  public enum State {
    PENDING,
    ACCEPTED,
    REJECTED
  }

  private final String memberAddress;
  private final String contractAddress;
  private final State state;
  private final String secret;

  public String getMemberAddress() {
    return memberAddress;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public State getState() {
    return state;
  }

  public String getSecret() {
    return secret;
  }

  private DataUnionJoinRequest(
      final String memberAddress,
      final String contractAddress,
      final State state,
      final String secret) {
    this.memberAddress = memberAddress;
    this.contractAddress = contractAddress;
    this.state = state;
    this.secret = secret;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final DataUnionJoinRequest that = (DataUnionJoinRequest) obj;
    return Objects.equals(memberAddress, that.memberAddress)
        && Objects.equals(contractAddress, that.contractAddress)
        && state == that.state
        && Objects.equals(secret, that.secret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(memberAddress, contractAddress, state, secret);
  }

  @Override
  public String toString() {
    return String.format(
        "DataUnionJoinRequest{memberAddress='%s', contractAddress='%s', state=%s, secret='%s'}",
        memberAddress, contractAddress, state, secret);
  }

  public static class Builder {
    private String memberAddress;
    private String contractAddress;
    private State state;
    private String secret;

    public Builder withMemberAddress(final String memberAddress) {
      this.memberAddress = memberAddress;
      return this;
    }

    public Builder withContractAddress(final String contractAddress) {
      this.contractAddress = contractAddress;
      return this;
    }

    public Builder withState(final State state) {
      this.state = state;
      return this;
    }

    public Builder withSecret(final String secret) {
      this.secret = secret;
      return this;
    }

    public DataUnionJoinRequest createDataUnionJoinRequest() {
      return new DataUnionJoinRequest(memberAddress, contractAddress, state, secret);
    }
  }
}
