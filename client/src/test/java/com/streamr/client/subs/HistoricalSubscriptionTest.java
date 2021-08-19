package com.streamr.client.subs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.MessageHandler;
import com.streamr.client.options.ResendLastOption;
import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.EncryptionUtil;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.protocol.utils.GroupKeyStore;
import com.streamr.client.protocol.utils.KeyExchangeUtil;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoricalSubscriptionTest {
  private StreamMessage msg;
  private GroupKeyStore keyStore;
  private KeyExchangeUtil keyExchangeUtil;
  private List<StreamMessage> received;
  private HistoricalSubscription sub;
  private boolean doneHandlerCalled;

  @BeforeEach
  void setup() {
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    msg =
        new StreamMessage.Builder()
            .withMessageId(messageId)
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
        new HistoricalSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            new ResendLastOption(10),
            defaultGroupKeyRequestFunction);
    groupKeyFunctionCallCount[0] = 0;
    doneHandlerCalled = false;
  }

  private MessageHandler defaultHandler =
      new MessageHandler() {
        @Override
        public void onMessage(Subscription sub, StreamMessage message) {
          received.add(message);
        }

        @Override
        public void done(Subscription sub) {
          doneHandlerCalled = true;
        }
      };
  private final int[] groupKeyFunctionCallCount = new int[1];
  private BasicSubscription.GroupKeyRequestFunction defaultGroupKeyRequestFunction =
      new BasicSubscription.GroupKeyRequestFunction() {
        @Override
        public void apply(Address publisherId, List<String> groupKeyIds) {
          groupKeyFunctionCallCount[0]++;
        }
      };

  @Test
  void doesNotHandleRealtimeMessages_queued() {
    sub.handleRealTimeMessage(msg);
    assertTrue(received.isEmpty());
  }

  @Test
  void callsTheDoneHandlerOnlyWhenTheEncryptionQueueIsEmpty()
      throws InvalidGroupKeyException, InterruptedException {
    GroupKey key = GroupKey.generate();
    keyStore =
        new GroupKeyStore() {
          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return key;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    sub =
        new HistoricalSubscription(
            msg.getStreamId(),
            0,
            defaultHandler,
            keyStore,
            keyExchangeUtil,
            new ResendLastOption(10),
            defaultGroupKeyRequestFunction);
    msg = EncryptionUtil.encryptStreamMessage(msg, key);
    sub.handleResentMessage(msg); // queued

    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (groupKeyFunctionCallCount[0] == 1) {
                    break;
                  }
                  try {
                    Thread.sleep(10);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    sub.endResend();
    List<GroupKey> list = new ArrayList<>();
    list.add(key);
    sub.onNewKeysAdded(msg.getPublisherId(), list);
    assertEquals(1, received.size());
    assertTrue(doneHandlerCalled);
  }
}
