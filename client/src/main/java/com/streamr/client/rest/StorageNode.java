package com.streamr.client.rest;

import com.streamr.ethereum.common.Address;

public final class StorageNode {
  private final Address address;

  public StorageNode(final Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return this.address;
  }
}
