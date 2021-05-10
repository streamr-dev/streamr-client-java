package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

public final class DataUnionSecretResponse {
  private final String name;
  private final String contractAddress;
  private final String secret;

  private DataUnionSecretResponse(
      final String name, final String contractAddress, final String secret) {
    Objects.requireNonNull(name, "name");
    this.name = name;
    Objects.requireNonNull(contractAddress, "contractAddress");
    this.contractAddress = contractAddress;
    Objects.requireNonNull(secret, "secret");
    this.secret = secret;
  }

  public String getName() {
    return name;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public String getSecret() {
    return secret;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final DataUnionSecretResponse that = (DataUnionSecretResponse) obj;
    return Objects.equals(name, that.name)
        && Objects.equals(contractAddress, that.contractAddress)
        && Objects.equals(secret, that.secret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contractAddress, secret);
  }

  @Override
  public String toString() {
    return String.format(
        "DataUnionSecretResponse{name='%s', contractAddress='%s', secret='%s'}",
        name, contractAddress, secret);
  }

  public static class Builder {
    private String name;
    private String contractAddress;
    private String secret;

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withContractAddress(final String contractAddress) {
      this.contractAddress = contractAddress;
      return this;
    }

    public Builder withSecret(final String secret) {
      this.secret = secret;
      return this;
    }

    public DataUnionSecretResponse createDataUnionSecret() {
      return new DataUnionSecretResponse(name, contractAddress, secret);
    }
  }
}
