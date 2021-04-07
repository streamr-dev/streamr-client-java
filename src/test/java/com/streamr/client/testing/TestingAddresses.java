package com.streamr.client.testing;

import com.streamr.client.utils.Address;
import java.util.Random;

public final class TestingAddresses {
  public static final Address SUBSCRIBER_ID = new Address("subscriberId");
  public static final Address PUBLISHER_ID = new Address("publisherId");

  private TestingAddresses() {}

  public static Address createSubscriberId(final int number) {
    return new Address("subscriberId" + String.valueOf(number));
  }

  public static Address createPublisherId(final int number) {
    return new Address("publisherId" + String.valueOf(number));
  }

  public static Address createRandom() {
    byte[] array = new byte[20];
    new Random().nextBytes(array);
    return new Address(array);
  }
}
