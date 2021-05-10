package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;

public final class DataUnionSecretRequest {
  private final String name;
  private final String contractAddress;

  private DataUnionSecretRequest(final String name, final String contractAddress) {
    Objects.requireNonNull(name, "name");
    this.name = name;
    Objects.requireNonNull(contractAddress, "contractAddress");
    this.contractAddress = contractAddress;
  }

  public String getName() {
    return name;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final DataUnionSecretRequest that = (DataUnionSecretRequest) obj;
    return Objects.equals(name, that.name) && Objects.equals(contractAddress, that.contractAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contractAddress);
  }

  @Override
  public String toString() {
    return String.format(
        "DataUnionSecretRequest{name='%s', contractAddress='%s'}", name, contractAddress);
  }

  public static class Builder {
    private String name;
    private String contractAddress;

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withContractAddress(final String contractAddress) {
      this.contractAddress = contractAddress;
      return this;
    }

    public DataUnionSecretRequest createDataUnionSecret() {
      return new DataUnionSecretRequest(name, contractAddress);
    }
  }
}
