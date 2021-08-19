package com.streamr.client.subs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.protocol.utils.GroupKeyStore;
import com.streamr.client.protocol.utils.KeyExchangeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionsTest {
  private Subscriptions subs;
  private GroupKeyStore keyStore;
  private KeyExchangeUtil keyExchangeUtil;

  private RealTimeSubscription createSub(String streamId, int partition) {
    return new RealTimeSubscription(streamId, partition, null, keyStore, keyExchangeUtil, null);
  }

  @BeforeEach
  void setup() {
    subs = new Subscriptions();
    keyStore =
        new GroupKeyStore() {
          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return null;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    keyExchangeUtil = new KeyExchangeUtil(null, null, null, null, null, null, null);
  }

  @Test
  void subscriptionsCanBeAddedAndRetrieved() throws SubscriptionNotFoundException {
    Subscription sub = createSub("stream", 0);
    subs.add(sub);
    // the original instance is returned when querying
    assertEquals(sub, subs.get(sub.getStreamId(), sub.getPartition()));
  }

  @Test
  void gettingNonExistentSubThrows() {
    Subscription sub = createSub("stream", 0);
    subs.add(sub);
    assertThrows(
        SubscriptionNotFoundException.class,
        () -> {
          subs.get("other", 0);
        });
  }

  @Test
  void subscriptionsCanBeAddedAndRemoved() throws SubscriptionNotFoundException {
    Subscription sub = createSub("stream", 0);
    subs.add(sub);

    // the original instance is returned when querying
    assertEquals(sub, subs.get(sub.getStreamId(), sub.getPartition()));
    subs.remove(sub);
    assertThrows(
        SubscriptionNotFoundException.class,
        () -> {
          subs.get(sub.getStreamId(), sub.getPartition());
        });
  }

  @Test
  void getAllForStreamId() {
    Subscription sub0 = createSub("stream", 0);
    Subscription sub3 = createSub("stream", 3);
    Subscription sub4 = createSub("stream", 4);
    Subscription otherSub = createSub("otherStream", 1);

    subs.add(sub0);
    subs.add(sub3);
    subs.add(sub4);
    subs.add(otherSub);

    List<Subscription> expected = new ArrayList<>();
    expected.add(sub0);
    expected.add(sub3);
    expected.add(sub4);
    assertArrayEquals(expected.toArray(), subs.getAllForStreamId("stream").toArray());
  }

  @Test
  void forEach() {
    Subscription sub1 = createSub("stream1", 5);
    Subscription sub2 = createSub("stream2", 2);
    List<Subscription> called = new ArrayList<>();
    Consumer<Subscription> f =
        new Consumer<Subscription>() {
          @Override
          public void accept(Subscription subscription) {
            called.add(subscription);
          }
        };

    subs.add(sub1);
    subs.add(sub2);
    subs.forEach(f);

    List<Subscription> expected = new ArrayList<>();
    expected.add(sub1);
    expected.add(sub2);
    assertEquals(expected, called);
  }
}
