package com.streamr.client.testing;

import com.streamr.ethereum.common.Address;
import java.util.Random;

public final class TestingAddresses {
  public static final Address SUBSCRIBER_ID =
      new Address("0x5555555555555555555555555555555555555555");
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
  public static final Address JAMES = new Address("0x4c5d64cae13ab2b3f42dce23238fb7d6fc28cb2b");

  private TestingAddresses() {}

  public static Address createSubscriberId(final int number) {
    if (number > 15) {
      throw new AssertionError("subscriberId number must be less than 16");
    }
    return new Address("0x555555555555555555555555555555555555555" + Integer.toHexString(number));
  }

  public static Address createPublisherId(final int number) {
    if (number > 15) {
      throw new AssertionError("publisherId number must be less than 16");
    }
    return new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB" + Integer.toHexString(number));
  }

  public static Address createRandom() {
    byte[] array = new byte[20];
    new Random().nextBytes(array);
    return new Address(array);
  }
}
