package com.streamr.client.subs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.streamr.client.MessageHandler;
import com.streamr.client.options.ResendLastOption;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMessageRef;
import com.streamr.client.utils.OrderedMsgChain;
import org.junit.jupiter.api.Test;

class CombinedSubscriptionTest {
  @Test
  void callsTheGapHandlerIfGapAmongRealTimeMessagesQueuedDuringResend()
      throws InterruptedException {
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
    StreamMessage afterMsg1 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
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
    CombinedSubscription sub =
        new CombinedSubscription(
            msg1.getStreamId(),
            0,
            new MessageHandler() {
              @Override
              public void onMessage(Subscription sub, StreamMessage message) {}
            },
            null /*Mock(GroupKeyStore)*/,
            null /*Mock(KeyExchangeUtil)*/,
            new ResendLastOption(10),
            null,
            10L,
            10L,
            false);
    final GapDetectedException[] ex = new GapDetectedException[1];
    sub.setGapHandler(
        new OrderedMsgChain.GapHandlerFunction() {
          @Override
          public void apply(
              MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
            ex[0] =
                new GapDetectedException(
                    sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId);
          }
        });
    sub.handleResentMessage(msg1);
    sub.handleRealTimeMessage(msg4);
    sub.endResend();
    Thread.sleep(50L);
    sub.clear();
    assertEquals(msg1.getStreamId(), ex[0].getStreamId());
    assertEquals(msg1.getStreamPartition(), ex[0].getStreamPartition());
    assertEquals(afterMsg1.getMessageRef(), ex[0].getFrom());
    assertEquals(msg4.getPreviousMessageRef(), ex[0].getTo());
    assertEquals(msg1.getPublisherId(), ex[0].getPublisherId());
    assertEquals(msg1.getMsgChainId(), ex[0].getMsgChainId());
  }
}
