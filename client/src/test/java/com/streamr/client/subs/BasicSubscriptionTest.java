package com.streamr.client.subs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.MessageHandler;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.EncryptedGroupKey;
import com.streamr.client.protocol.utils.EncryptionUtil;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.protocol.utils.GroupKeyStore;
import com.streamr.client.protocol.utils.KeyExchangeUtil;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMessageRef;
import com.streamr.client.utils.OrderedMsgChain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BasicSubscription is abstract, but contains most of the code for RealtimeSubscription and
 * HistoricalSubscription. This class tests BasicSubscription via RealtimeSubscription.
 */
class BasicSubscriptionTest {
  private static final long propagationTimeout = 1000;
  private static final long resendTimeout = 1000;

  private StreamMessage msg;
  private GroupKeyStore keyStore;
  private KeyExchangeUtil keyExchangeUtil;
  private List<StreamMessage> received;
  private RealTimeSubscription sub;
  private int groupKeyRequestCount;
  private int unableToDecryptCount;
  private MessageHandler defaultHandler =
      new MessageHandler() {
        @Override
        public void onMessage(Subscription sub, StreamMessage message) {
          received.add(message);
        }

        @Override
        public void onUnableToDecrypt(UnableToDecryptException e) {
          unableToDecryptCount++;
        }
      };
  private BasicSubscription.GroupKeyRequestFunction defaultGroupKeyRequestFunction =
      new BasicSubscription.GroupKeyRequestFunction() {
        @Override
        public void apply(Address publisherId, List<String> groupKeyIds) {
          groupKeyRequestCount++;
        }
      };

  @BeforeEach
  void setup() {
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    msg =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
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
    received = new ArrayList<>();
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);
    groupKeyRequestCount = 0;
    unableToDecryptCount = 0;
  }

  @Test
  void callsTheMessageHandlerWhenRealtimeMessagesAreReceived() {
    sub.handleRealTimeMessage(msg);
    assertEquals(msg, received.get(0));
  }

  @Test
  void callsTheHandlerOnceForEachMessageInOrder() {
    List<StreamMessage> msgs = new ArrayList<>();
    for (long i = 0; i < 5; i++) {
      final MessageId messageId =
          new MessageId.Builder()
              .withStreamId("streamId")
              .withTimestamp(i)
              .withSequenceNumber(0)
              .withPublisherId(TestingAddresses.PUBLISHER_ID)
              .withMsgChainId("msgChainId")
              .createMessageId();
      msgs.add(
          new StreamMessage.Builder()
              .withMessageId(messageId)
              .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0L))
              .withContent(TestingContent.emptyMessage())
              .createStreamMessage());
    }
    for (int i = 0; i < 5; i++) {
      sub.handleRealTimeMessage(msgs.get(i));
    }
    for (int i = 0; i < 5; i++) {
      assertEquals(received.get(i).serialize(), msgs.get(i).serialize());
    }
  }

  @Test
  void handlesResentMessagesDuringResending() {
    sub.setResending(true);
    sub.handleResentMessage(msg);
    assertEquals(msg, received.get(0));
  }

  @Test
  void ignoresDuplicateMessages() {
    sub.handleRealTimeMessage(msg);
    sub.handleRealTimeMessage(msg);
    assertEquals(1, received.size());
  }

  @Test
  void callsTheGapHandlerIfGapIsDetected() throws InterruptedException {
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(4)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg4 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(3L, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final GapDetectedException[] ex = new GapDetectedException[1];
    sub.setGapHandler(
        new OrderedMsgChain.GapHandlerFunction() {
          @Override
          public void apply(
              MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
            // GapDetectedException is used just to store the values the gap handler is called with
            ex[0] =
                new GapDetectedException(
                    sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId);
          }
        });
    sub.handleRealTimeMessage(msg1);
    assertEquals(null, ex[0]);
    sub.handleRealTimeMessage(msg4);
    Thread.sleep(5 * propagationTimeout); // make sure the propagationTimeout has passed and the gap
    // handler triggered
    assertEquals(msg1.getStreamId(), ex[0].getStreamId());
    assertEquals(msg1.getStreamPartition(), ex[0].getStreamPartition());
    assertEquals(
        new MessageRef(msg1.getTimestamp(), msg1.getSequenceNumber() + 1), ex[0].getFrom());
    assertEquals(msg4.getPreviousMessageRef(), ex[0].getTo());
    assertEquals(msg1.getPublisherId(), ex[0].getPublisherId());
    assertEquals(msg1.getMsgChainId(), ex[0].getMsgChainId());
  }

  @Test
  void doesNotThrowIfDifferentPublishers() {
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(4)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg4 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(3L, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    sub.handleRealTimeMessage(msg1);
    sub.handleRealTimeMessage(msg4);
  }

  @Test
  void callsTheGapHandlerIfGapIsDetected_sameTimestampButDifferentSequenceNumbers()
      throws InterruptedException {
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(4)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg4 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(1L, 3L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final GapDetectedException[] ex = new GapDetectedException[1];
    sub.setGapHandler(
        new OrderedMsgChain.GapHandlerFunction() {
          @Override
          public void apply(
              MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
            // GapDetectedException is used just to store the values the gap handler is called with
            ex[0] =
                new GapDetectedException(
                    sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId);
          }
        });

    sub.handleRealTimeMessage(msg1);
    assertNull(ex[0]);
    sub.handleRealTimeMessage(msg4);
    Thread.sleep(5 * propagationTimeout); // make sure the propagationTimeout has passed and the gap
    // handler triggered
    assertEquals(msg1.getStreamId(), ex[0].getStreamId());
    assertEquals(msg1.getStreamPartition(), ex[0].getStreamPartition());
    assertEquals(
        new MessageRef(msg1.getTimestamp(), msg1.getSequenceNumber() + 1), ex[0].getFrom());
    assertEquals(msg4.getPreviousMessageRef(), ex[0].getTo());
    assertEquals(msg1.getPublisherId(), ex[0].getPublisherId());
    assertEquals(msg1.getMsgChainId(), ex[0].getMsgChainId());
  }

  @Test
  void doesNotThrowIfThereINoGap() throws InterruptedException {
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(1)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(1L, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(4)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg3 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(1L, 1L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();

    sub.setGapHandler(
        new OrderedMsgChain.GapHandlerFunction() {
          @Override
          public void apply(
              MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
            throw new GapDetectedException(
                sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId);
          }
        });
    sub.handleRealTimeMessage(msg1);
    sub.handleRealTimeMessage(msg2);
    sub.handleRealTimeMessage(msg3);
    Thread.sleep(
        5 * propagationTimeout); // make sure the propagationTimeout has passed to allow the gap
    // handler to trigger
    assertEquals(3, received.size());
  }

  @Test
  void decryptsEncryptedMessagesWithTheCorrectKey() throws InvalidGroupKeyException {
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(System.currentTimeMillis())
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    Map<String, Object> plaintext = TestingContent.mapWithValue("foo", "bar");
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withContent(TestingContent.fromJsonMap(plaintext))
            .createStreamMessage();
    final GroupKey groupKey = GroupKey.generate();
    msg1 = EncryptionUtil.encryptStreamMessage(msg1, groupKey);
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 0) {
              throw new RuntimeException("GroupKey.get(...) called more that once!");
            }
            getCallCount++;
            return groupKey;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);
    sub.handleRealTimeMessage(msg1);
    // 1 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> groupKey
    assertEquals(plaintext, received.get(0).getParsedContent());
  }

  @Test
  void reportsNewGroupKeysToTheKeyExchangeUtil() throws InvalidGroupKeyException {
    GroupKey oldKey = GroupKey.generate();
    GroupKey newKey = GroupKey.generate();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    msg = EncryptionUtil.encryptStreamMessage(msg, oldKey);
    msg =
        new StreamMessage.Builder(msg)
            .withNewGroupKey(EncryptionUtil.encryptGroupKey(newKey, oldKey))
            .createStreamMessage();
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 0) {
              throw new RuntimeException("GroupKey.get(...) called more that once!");
            }
            getCallCount++;
            return oldKey;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    final boolean[] handleNewAESEncryptedKeysCalled = {false};
    keyExchangeUtil =
        new KeyExchangeUtil(keyStore, null, null, null, null, null, null) {
          @Override
          public void handleNewAESEncryptedKeys(
              final List<EncryptedGroupKey> encryptedKeys,
              final String streamId,
              final Address publisherId,
              final String groupKeyId) {
            handleNewAESEncryptedKeysCalled[0] = true;
          }
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);
    sub.handleRealTimeMessage(msg);

    // 1 * keyStore.get(msg.getStreamId(), oldKey.getGroupKeyId()) >> oldKey
    // 1 * keyExchangeUtil.handleNewAESEncryptedKeys([msg.getNewGroupKey()], msg.getStreamId(),
    // msg.getPublisherId(), msg.getGroupKeyId())
    assertTrue(handleNewAESEncryptedKeysCalled[0]);
  }

  @Test
  void callsKeyRequestFunctionIfTheKeyIsNotInTheKeyStore_multipleTimesIfTheresNoResponse()
      throws InvalidGroupKeyException, InterruptedException {
    GroupKey groupKey = GroupKey.generate();
    msg = EncryptionUtil.encryptStreamMessage(msg, groupKey);
    final Address[] receivedPublisherId = {null};
    final int[] nbCalls = {0};
    int timeout = 3000;
    RealTimeSubscription sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            new BasicSubscription.GroupKeyRequestFunction() {
              @Override
              public void apply(Address publisherId, List<String> groupKeyIds) {
                receivedPublisherId[0] = publisherId;
                nbCalls[0]++;
              }
            },
            timeout,
            5000,
            false);
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 2) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return null;
              case 2:
                return groupKey;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };

    // First call to groupKeyRequestFunction
    sub.handleRealTimeMessage(msg);
    // Wait for 2 timeouts to happen
    Thread.sleep(timeout * 2 + 1500);

    // 1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in
    // store
    assertEquals(3, nbCalls[0]);
    List<GroupKey> list = new ArrayList<>();
    list.add(groupKey);
    sub.onNewKeysAdded(msg.getPublisherId(), list);
    Thread.sleep(timeout * 2);

    // 1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> groupKey // key is now found
    assertEquals(msg.getPublisherId(), receivedPublisherId[0]);
    assertEquals(3, nbCalls[0]);
  }

  @Test
  void callsKeyRequestFunctionMAX_NB_GROUP_KEY_REQUESTStimes()
      throws InvalidGroupKeyException, InterruptedException {
    GroupKey groupKey = GroupKey.generate();
    msg = EncryptionUtil.encryptStreamMessage(msg, groupKey);

    final int[] nbCalls = {0};
    int timeout = 200;
    RealTimeSubscription sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            new BasicSubscription.GroupKeyRequestFunction() {
              @Override
              public void apply(Address publisherId, List<String> groupKeyIds) {
                nbCalls[0]++;
              }
            },
            timeout,
            5000,
            false);

    sub.handleRealTimeMessage(msg);
    Thread.sleep(timeout * (BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS + 2));

    // 1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in
    // store
    assertEquals(BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS, nbCalls[0]);
  }

  @Test
  void queuesMessagesWhenNotAbleToDecryptAndHandlesThemOnceTheKeyIsUpdated()
      throws InvalidGroupKeyException, InterruptedException {
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar1")))
            .createStreamMessage();
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar2")))
            .createStreamMessage();

    GroupKey groupKey = GroupKey.generate();
    msg1 = EncryptionUtil.encryptStreamMessage(msg1, groupKey);
    msg2 = EncryptionUtil.encryptStreamMessage(msg2, groupKey);
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 2) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return null;
              case 2:
                return groupKey;
              case 3:
                return groupKey;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);
    // Cannot decrypt msg1, queues it and calls the key request function
    sub.handleRealTimeMessage(msg1);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // 1 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in
    // store
    assertEquals(1, groupKeyRequestCount);
    // Cannot decrypt msg2, queues it but doesn't call the key request function
    sub.handleRealTimeMessage(msg2);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // 0 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) // not called because the
    // request for the key is in progress
    assertEquals(1, groupKeyRequestCount);
    // faking the reception of the group key response
    List<GroupKey> list = new ArrayList<>();
    list.add(groupKey);
    sub.onNewKeysAdded(msg1.getPublisherId(), list);

    // 2 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> groupKey // key now found
    // for both messages
    assertEquals(2, received.size());
    assertEquals(TestingContent.mapWithValue("foo", "bar1"), received.get(0).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar2"), received.get(1).getParsedContent());
    assertEquals(1, groupKeyRequestCount);
  }

  @Test
  void queuesMessagesWhenNotAbleToDecryptAndHandlesThemOnceTheKeyIsUpdated_multiplePublishers()
      throws InvalidGroupKeyException, InterruptedException {
    final MessageId messageId3 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1pub1 =
        new StreamMessage.Builder()
            .withMessageId(messageId3)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar1")))
            .createStreamMessage();
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2pub1 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar2")))
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1pub2 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar3")))
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2pub2 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar4")))
            .createStreamMessage();

    GroupKey groupKeyPub1 = GroupKey.generate();
    GroupKey groupKeyPub2 = GroupKey.generate();

    msg1pub1 = EncryptionUtil.encryptStreamMessage(msg1pub1, groupKeyPub1);
    msg2pub1 = EncryptionUtil.encryptStreamMessage(msg2pub1, groupKeyPub1);

    msg1pub2 = EncryptionUtil.encryptStreamMessage(msg1pub2, groupKeyPub2);
    msg2pub2 = EncryptionUtil.encryptStreamMessage(msg2pub2, groupKeyPub2);

    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 6) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return null;
              case 2:
                return null;
              case 3:
                return groupKeyPub1;
              case 4:
                return groupKeyPub1;
              case 5:
                return groupKeyPub2;
              case 6:
                return groupKeyPub2;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);
    // Cannot decrypt msg1pub1, queues it and calls the key request function
    sub.handleRealTimeMessage(msg1pub1);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // 1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> null // key not
    // found in store
    assertEquals(1, groupKeyRequestCount);
    // Cannot decrypt msg2pub1, queues it.
    sub.handleRealTimeMessage(msg2pub1);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);
    // 0 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) // not called because
    // the request for the key is in progress
    assertEquals(1, groupKeyRequestCount);
    // Cannot decrypt msg1pub2, queues it and calls the key request function
    sub.handleRealTimeMessage(msg1pub2);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // 1 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> null // key not
    // found in store
    assertEquals(2, groupKeyRequestCount);
    // Cannot decrypt msg2pub2, queues it.
    sub.handleRealTimeMessage(msg2pub2);
    assertEquals(2, groupKeyRequestCount);
    // 0 * keyStore.get(msg2pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) // not called because
    // the request for the key is in progress

    // faking the reception of the group key response
    List<GroupKey> l1 = new ArrayList<>();
    l1.add(groupKeyPub1);
    sub.onNewKeysAdded(msg1pub1.getPublisherId(), l1);
    List<GroupKey> l2 = new ArrayList<>();
    l2.add(groupKeyPub2);
    sub.onNewKeysAdded(msg1pub2.getPublisherId(), l2);

    // 2 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
    // 2 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> groupKeyPub2
    assertEquals(TestingContent.mapWithValue("foo", "bar1"), received.get(0).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar2"), received.get(1).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar3"), received.get(2).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar4"), received.get(3).getParsedContent());
    assertEquals(2, groupKeyRequestCount);
  }

  @Test
  void
      queuesMessagesWhenNotAbleToDecryptAndHandlesThemOnceTheKeyIsUpdated_multiplePublishersInterleaved()
          throws InvalidGroupKeyException, InterruptedException {
    final MessageId messageId4 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1pub1 =
        new StreamMessage.Builder()
            .withMessageId(messageId4)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar1")))
            .createStreamMessage();
    final MessageId messageId3 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2pub1 =
        new StreamMessage.Builder()
            .withMessageId(messageId3)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar2")))
            .createStreamMessage();
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(3)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg3pub1 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar3")))
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg1pub2 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar4")))
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(2))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg2pub2 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("foo", "bar5")))
            .createStreamMessage();

    GroupKey groupKeyPub1 = GroupKey.generate();
    GroupKey groupKeyPub2 = GroupKey.generate();

    // [msg1pub1, msg2pub1, msg3pub1].each {it = EncryptionUtil.encryptStreamMessage(it,
    // groupKeyPub1) }
    // msg1pub1 = EncryptionUtil.encryptStreamMessage(msg1pub1, groupKeyPub1);
    // msg2pub1 = EncryptionUtil.encryptStreamMessage(msg2pub1, groupKeyPub1);
    // msg3pub1 = EncryptionUtil.encryptStreamMessage(msg3pub1, groupKeyPub1);

    msg1pub1 = EncryptionUtil.encryptStreamMessage(msg1pub1, groupKeyPub1);
    msg2pub1 = EncryptionUtil.encryptStreamMessage(msg2pub1, groupKeyPub1);
    msg3pub1 = EncryptionUtil.encryptStreamMessage(msg3pub1, groupKeyPub1);

    msg1pub2 = EncryptionUtil.encryptStreamMessage(msg1pub2, groupKeyPub2);
    msg2pub2 = EncryptionUtil.encryptStreamMessage(msg2pub2, groupKeyPub2);
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 7) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return null;
              case 2:
                return null;
              case 3:
                return groupKeyPub1;
              case 4:
                return groupKeyPub1;
              case 5:
                return groupKeyPub1;
              case 6:
                return groupKeyPub2;
              case 7:
                return groupKeyPub2;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);

    sub.handleRealTimeMessage(msg1pub1);
    sub.handleRealTimeMessage(msg1pub2);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // TODO: 1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> null // key
    // not found in store
    // TODO: 1 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> null // key
    // not found in store
    assertEquals(2, groupKeyRequestCount);
    sub.handleRealTimeMessage(msg2pub1); // queued
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);
    assertEquals(2, groupKeyRequestCount);
    // TODO: 0 * keyStore.get(msg2pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) // not called
    // because the request for the key is in progress

    // Triggers processing of queued messages for pub1
    List<GroupKey> l0 = new ArrayList<>();
    l0.add(groupKeyPub1);
    sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), l0);

    // TODO:  2 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
    assertEquals(2, groupKeyRequestCount);
    assertEquals(2, received.size());
    assertEquals(TestingContent.mapWithValue("foo", "bar1"), received.get(0).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar2"), received.get(1).getParsedContent());

    // Processed immediately because now we have the key
    sub.handleRealTimeMessage(msg3pub1);

    // TODO: 1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
    assertEquals(3, received.size());
    assertEquals(TestingContent.mapWithValue("foo", "bar3"), received.get(2).getParsedContent());

    sub.handleRealTimeMessage(msg2pub2); // queued, because no key for pub2 yet
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);
    assertEquals(3, received.size());
    assertEquals(2, groupKeyRequestCount);
    // Triggers processing of queued messages for pub2
    List<GroupKey> l1 = new ArrayList<>();
    l1.add(groupKeyPub2);
    sub.onNewKeysAdded(TestingAddresses.createPublisherId(2), l1);

    // TODO: 2 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> groupKeyPub2
    assertEquals(5, received.size());
    assertEquals(TestingContent.mapWithValue("foo", "bar4"), received.get(3).getParsedContent());
    assertEquals(TestingContent.mapWithValue("foo", "bar5"), received.get(4).getParsedContent());
    assertEquals(2, groupKeyRequestCount);
  }

  @Test
  void
      queuesMessagesWhenNotAbleToDecryptAndHandlesThemOnceTheKeyIsUpdated_onePublisher_twoKeysOnTwoMsgChains()
          throws InvalidGroupKeyException, InterruptedException {
    // All messages have the same publisherId
    final MessageId messageId3 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage key1msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId3)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("n", 1.0)))
            .createStreamMessage();
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(2)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChain1")
            .createMessageId();
    StreamMessage key1msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("n", 2.0)))
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(3)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChain2")
            .createMessageId();
    StreamMessage key2msg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("n", 3.0)))
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(4)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.createPublisherId(1))
            .withMsgChainId("msgChain2")
            .createMessageId();
    StreamMessage key2msg2 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.fromJsonMap(TestingContent.mapWithValue("n", 4.0)))
            .createStreamMessage();
    GroupKey key1 = GroupKey.generate();
    GroupKey key2 = GroupKey.generate();
    key1msg1 = EncryptionUtil.encryptStreamMessage(key1msg1, key1);
    key1msg2 = EncryptionUtil.encryptStreamMessage(key1msg2, key1);
    key2msg1 = EncryptionUtil.encryptStreamMessage(key2msg1, key2);
    key2msg2 = EncryptionUtil.encryptStreamMessage(key2msg2, key2);

    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 6) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return null;
              case 2:
                return null;
              case 3:
                return key1;
              case 4:
                return key1;
              case 5:
                return key2;
              case 6:
                return key2;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new RealTimeSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            defaultGroupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            false);

    sub.handleRealTimeMessage(key1msg1);
    sub.handleRealTimeMessage(key2msg1);
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);

    // TODO: 1 * keyStore.get(key1msg1.getStreamId(), key1.getGroupKeyId()) >> null // key not found
    // in store
    // TODO: 1 * keyStore.get(key2msg1.getStreamId(), key2.getGroupKeyId()) >> null // key not found
    // in store
    assertEquals(2, groupKeyRequestCount);

    sub.handleRealTimeMessage(key1msg2); // queued
    sub.handleRealTimeMessage(key2msg2); // queued
    // group key request function gets called asynchronously, make sure there's time to call it
    Thread.sleep(100);
    assertEquals(2, groupKeyRequestCount);
    // TODO: 0 * keyStore.get(_, _); // not called because the request for both keys is in progress

    // Triggers processing of queued messages for key1 / msgChain1
    List<GroupKey> keys = new ArrayList<>();
    keys.add(key1);
    sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), keys);

    // TODO: 2 * keyStore.get(key1msg1.getStreamId(), key1.getGroupKeyId()) >> key1
    assertEquals(2, groupKeyRequestCount);
    assertEquals(2, received.size());
    assertEquals(TestingContent.mapWithValue("n", 1.0), received.get(0).getParsedContent());
    assertEquals(TestingContent.mapWithValue("n", 2.0), received.get(1).getParsedContent());

    // Triggers processing of queued messages for key2 / msgChain2
    List<GroupKey> list = new ArrayList<>();
    list.add(key2);
    sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), list);

    // TODO: 2 * keyStore.get(key2msg1.getStreamId(), key2.getGroupKeyId()) >> key2
    assertEquals(4, received.size());
    assertEquals(TestingContent.mapWithValue("n", 3.0), received.get(2).getParsedContent());
    assertEquals(TestingContent.mapWithValue("n", 4.0), received.get(3).getParsedContent());

    assertEquals(2, groupKeyRequestCount);
  }

  @Test
  void callsOnUnableToDecryptHandlerWhenNotAbleToDecryptAfterReceivingKey()
      throws InvalidGroupKeyException {
    GroupKey correctGroupKey = GroupKey.generate();
    GroupKey incorrectGroupKeyWithCorrectId =
        new GroupKey(correctGroupKey.getGroupKeyId(), GroupKey.generate().getGroupKeyHex());
    msg = EncryptionUtil.encryptStreamMessage(msg, correctGroupKey);
    sub.handleRealTimeMessage(msg); // queues message
    List<GroupKey> list = new ArrayList<>();
    list.add(incorrectGroupKeyWithCorrectId);
    sub.onNewKeysAdded(msg.getPublisherId(), list);
    assertEquals(1, unableToDecryptCount);
  }

  // TODO: good test?
  @Test
  void doesntThrowWhenSomeOtherKeyIsAdded() throws InvalidGroupKeyException {
    GroupKey correctGroupKey = GroupKey.generate();
    GroupKey otherKey = GroupKey.generate();
    msg = EncryptionUtil.encryptStreamMessage(msg, correctGroupKey);
    sub.handleRealTimeMessage(msg); // queues message
    List<GroupKey> list = new ArrayList<>();
    list.add(otherKey);
    sub.onNewKeysAdded(msg.getPublisherId(), list); // other key added (not waiting for this key)
  }
}
