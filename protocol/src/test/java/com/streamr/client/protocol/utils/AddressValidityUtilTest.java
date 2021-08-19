package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AddressValidityUtilTest {
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

  @Test
  void isValidSubscriber() {
    final int[] getSubscribersFunctionCallCount = {0};
    Function<String, List<Address>> getSubscribersFunction =
        new Function<String, List<Address>>() {
          @Override
          public List<Address> apply(String streamId) {
            List<Address> addresses = Arrays.asList(createSubscriberId(1), createSubscriberId(2));
            getSubscribersFunctionCallCount[0]++;
            if (streamId.equals("streamId")) {
              return addresses;
            } else {
              return null;
            }
          }
        };
    final int[] isSubscriberFunctionCallCount = {0};
    BiFunction<String, Address, Boolean> isSubscriberFunction =
        new BiFunction<String, Address, Boolean>() {
          @Override
          public Boolean apply(String streamId, Address subscriber) {
            isSubscriberFunctionCallCount[0]++;
            return streamId.equals("streamId") && subscriber.equals(createSubscriberId(3));
          }
        };
    AddressValidityUtil util =
        new AddressValidityUtil(getSubscribersFunction, isSubscriberFunction, null, null);

    // cache miss --> getting all addresses
    boolean res1 = util.isValidSubscriber("streamId", createSubscriberId(1));
    // cache hit
    boolean res2 = util.isValidSubscriber("streamId", createSubscriberId(2));
    // cache miss --> get only this address
    boolean res3 = util.isValidSubscriber("streamId", createSubscriberId(3));
    // cache miss --> get only this address
    boolean res4 = util.isValidSubscriber("streamId", createSubscriberId(4));
    // cache hit
    boolean res5 = util.isValidSubscriber("streamId", createSubscriberId(1));
    // cache hit
    boolean res6 = util.isValidSubscriber("streamId", createSubscriberId(3));
    // cache hit
    boolean res7 = util.isValidSubscriber("streamId", createSubscriberId(4));

    assertEquals(1, getSubscribersFunctionCallCount[0]);
    assertEquals(2, isSubscriberFunctionCallCount[0]);
    assertTrue(res1);
    assertTrue(res2);
    assertTrue(res3);
    assertTrue(!res4);
    assertTrue(res5);
    assertTrue(res6);
    assertTrue(!res7);
  }

  @Test
  void isValidPublisher() {
    final int[] getPublishersFunctionCallCount = {0};
    Function<String, List<Address>> getPublishersFunction =
        new Function<String, List<Address>>() {
          @Override
          public List<Address> apply(String streamId) {
            getPublishersFunctionCallCount[0]++;
            if (streamId.equals("streamId")) {
              return Arrays.asList(createPublisherId(1), createPublisherId(2));
            } else {
              return null;
            }
          }
        };
    final int[] isPublisherFunctionCallCount = {0};
    BiFunction<String, Address, Boolean> isPublisherFunction =
        new BiFunction<String, Address, Boolean>() {
          @Override
          public Boolean apply(String streamId, Address publisher) {
            isPublisherFunctionCallCount[0]++;
            return streamId.equals("streamId") && publisher.equals(createPublisherId(3));
          }
        };
    AddressValidityUtil util =
        new AddressValidityUtil(null, null, getPublishersFunction, isPublisherFunction);

    // cache miss --> getting all addresses
    boolean res1 = util.isValidPublisher("streamId", createPublisherId(1));
    // cache hit
    boolean res2 = util.isValidPublisher("streamId", createPublisherId(2));
    // cache miss --> get only this address
    boolean res3 = util.isValidPublisher("streamId", createPublisherId(3));
    // cache miss --> get only this address
    boolean res4 = util.isValidPublisher("streamId", createPublisherId(4));
    // cache hit
    boolean res5 = util.isValidPublisher("streamId", createPublisherId(1));
    // cache hit
    boolean res6 = util.isValidPublisher("streamId", createPublisherId(3));
    // cache hit
    boolean res7 = util.isValidPublisher("streamId", createPublisherId(4));

    assertEquals(1, getPublishersFunctionCallCount[0]);
    assertEquals(2, isPublisherFunctionCallCount[0]);
    assertTrue(res1);
    assertTrue(res2);
    assertTrue(res3);
    assertTrue(!res4);
    assertTrue(res5);
    assertTrue(res6);
    assertTrue(!res7);
  }

  @Test
  void nbSubscribersToRevoke() {
    final int[] streamId1CallCount = {0};
    final int[] streamId2CallCount = {0};
    Function<String, List<Address>> getSubscribersFunction =
        new Function<String, List<Address>>() {
          @Override
          public List<Address> apply(String streamId) {
            if (streamId.equals("streamId1")) {
              streamId1CallCount[0]++;
              switch (streamId1CallCount[0]) {
                case 1:
                  return Arrays.asList(createSubscriberId(1), createSubscriberId(2));
                case 2:
                  return Arrays.asList(createSubscriberId(1), createSubscriberId(3));
                case 3:
                  return Arrays.asList(
                      createSubscriberId(1), createSubscriberId(3), createSubscriberId(8));
                case 4:
                  return Arrays.asList(
                      createSubscriberId(4), createSubscriberId(3), createSubscriberId(2));
              }
            } else if (streamId.equals("streamId2")) {
              streamId2CallCount[0]++;
              switch (streamId2CallCount[0]) {
                case 1:
                  return Arrays.asList(createSubscriberId(1), createSubscriberId(2));
                case 2:
                  return Arrays.asList(createSubscriberId(1), createSubscriberId(2));
                case 3:
                  return Arrays.asList(
                      createSubscriberId(5), createSubscriberId(3), createSubscriberId(8));
                case 4:
                  return Arrays.asList(
                      createSubscriberId(9), createSubscriberId(10), createSubscriberId(11));
              }
            }
            return null;
          }
        };
    AddressValidityUtil util = new AddressValidityUtil(getSubscribersFunction, null, null, null);

    assertEquals(0, util.nbSubscribersToRevoke("streamId1"));
    assertEquals(0, util.nbSubscribersToRevoke("streamId2"));
    assertEquals(1, util.nbSubscribersToRevoke("streamId1"));
    assertEquals(0, util.nbSubscribersToRevoke("streamId2"));
    assertEquals(0, util.nbSubscribersToRevoke("streamId1"));
    assertEquals(2, util.nbSubscribersToRevoke("streamId2"));
    assertEquals(2, util.nbSubscribersToRevoke("streamId1"));
    assertEquals(3, util.nbSubscribersToRevoke("streamId2"));
  }
}
