package com.streamr.client.rest;

import com.streamr.client.java.util.Objects;
import java.util.List;

public final class Publishers {
  private final List<String> addresses;

  public Publishers(final List<String> addresses) {
    Objects.requireNonNull(addresses);
    this.addresses = addresses;
  }

  public List<String> getAddresses() {
    return addresses;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final Publishers that = (Publishers) obj;
    return Objects.equals(addresses, that.addresses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addresses);
  }
}
