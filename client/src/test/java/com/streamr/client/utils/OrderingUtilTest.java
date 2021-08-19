package com.streamr.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMessageRef;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class OrderingUtilTest {
  private final StreamMessage.Content content = TestingContent.emptyMessage();
  private final StreamMessage msg1 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(1)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withContent(content)
          .createStreamMessage();
  private final StreamMessage msg2 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(2)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(1L, 0L))
          .withContent(content)
          .createStreamMessage();
  private final StreamMessage msg3 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(3)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(2L, 0L))
          .withContent(content)
          .createStreamMessage();
  private final StreamMessage msg4 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(4)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(3L, 0L))
          .withContent(content)
          .createStreamMessage();

  @Test
  void callsTheMessageHandlerWhenMessageIsReceived() {
    final StreamMessage[] received = new StreamMessage[1];
    OrderingUtil util =
        new OrderingUtil(
            "streamId",
            0,
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received[0] = streamMessage;
              }
            },
            null,
            5000L,
            5000L,
            false);

    util.add(msg1);
    assertEquals(msg1, received[0]);
  }

  @Test
  void callsGapHandlerWhenGapIsDetected() throws InterruptedException {
    final MessageRef[] fromReceived = new MessageRef[1];
    final MessageRef[] toReceived = new MessageRef[1];
    final Address[] publisherIdReceived = new Address[1];
    final String[] msgChainIdReceived = new String[1];

    OrderingUtil util =
        new OrderingUtil(
            "streamId",
            0,
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {}
            },
            new OrderedMsgChain.GapHandlerFunction() {
              @Override
              public void apply(
                  MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                fromReceived[0] = from;
                toReceived[0] = to;
                publisherIdReceived[0] = publisherId;
                msgChainIdReceived[0] = msgChainId;
              }
            },
            100L,
            100L,
            false);

    util.add(msg1);
    util.add(msg4);
    Thread.sleep(150L);
    util.add(msg2);
    util.add(msg3);
    assertEquals(1L, fromReceived[0].getTimestamp());
    assertEquals(1L, fromReceived[0].getSequenceNumber());
    assertEquals(3L, toReceived[0].getTimestamp());
    assertEquals(0L, toReceived[0].getSequenceNumber());
    assertEquals(msg1.getPublisherId(), publisherIdReceived[0]);
    assertEquals(msg1.getMsgChainId(), msgChainIdReceived[0]);
  }

  @Test
  void doesNotCallGapHandlerWhenGapDetectedButResolvedBeforeRequestShouldBeSent()
      throws InterruptedException {
    final boolean[] called = {false};

    OrderingUtil util =
        new OrderingUtil(
            "streamId",
            0,
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {}
            },
            new OrderedMsgChain.GapHandlerFunction() {
              @Override
              public void apply(
                  MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                called[0] = true;
              }
            },
            1000L,
            1000L,
            false);

    util.add(msg1);
    util.add(msg4);
    util.add(msg2);
    util.add(msg3);
    Thread.sleep(1200L);

    assertTrue(!called[0]);
  }
}
