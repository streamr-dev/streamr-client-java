package com.streamr.client.rest;

import com.streamr.client.protocol.utils.Address;

public final class StorageNode {
  public static final StorageNode STREAMR_GERMANY =
      new StorageNode(new Address("0x31546eEA76F2B2b3C5cC06B1c93601dc35c9D916"));
  public static final StorageNode STREAMR_DOCKER_DEV =
      new StorageNode(new Address("0xde1112f631486CfC759A50196853011528bC5FA0"));

  private final Address address;

  public StorageNode(final Address address) {
    this.address = address;
  }

  public Address getAddress() {
    return this.address;
  }
}
