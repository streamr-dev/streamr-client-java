package com.streamr.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMessageRef;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrderedMsgChainTest {
  final StreamMessage.Content content = TestingContent.emptyMessage();
  final StreamMessage msg1 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(1)
                  .withSequenceNumber(0)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withContent(content)
          .createStreamMessage();
  final StreamMessage msg2 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(2)
                  .withSequenceNumber(0)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(1L, 0L))
          .withContent(content)
          .createStreamMessage();
  final StreamMessage msg3 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(3)
                  .withSequenceNumber(0)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(2L, 0L))
          .withContent(content)
          .createStreamMessage();
  final StreamMessage msg4 =
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
  final StreamMessage msg5 =
      new StreamMessage.Builder()
          .withMessageId(
              new MessageId.Builder()
                  .withTimestamp(5)
                  .withStreamId("streamId")
                  .withPublisherId(TestingAddresses.PUBLISHER_ID)
                  .withMsgChainId("msgChainId")
                  .createMessageId())
          .withPreviousMessageRef(TestingMessageRef.createMessageRef(4L, 0L))
          .withContent(content)
          .createStreamMessage();

  final Address publisherId = TestingAddresses.PUBLISHER_ID;

  @Test
  void handlesOrderedMessagesInOrder() {
    List<StreamMessage> received = new ArrayList<>();
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            null,
            5000L,
            5000L,
            false);

    util.add(msg1);
    util.add(msg2);
    util.add(msg3);

    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    expected.add(msg2);
    expected.add(msg3);
    assertEquals(expected, received);
  }

  @Test
  void dropsDuplicates() {
    List<StreamMessage> received = new ArrayList<>();
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            null,
            5000L,
            5000L,
            false);

    util.add(msg1);
    util.add(msg1);
    util.add(msg2);
    util.add(msg1);

    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    expected.add(msg2);
    assertEquals(expected, received);
  }

  @Test
  void handlesUnorderedMessagesInOrder() {
    List<StreamMessage> received = new ArrayList<>();
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            new OrderedMsgChain.GapHandlerFunction() {
              @Override
              public void apply(
                  MessageRef from, MessageRef to, Address publisherId, String msgChainId) {}
            },
            5000L,
            5000L,
            false);

    util.add(msg1);
    util.add(msg2);
    util.add(msg5);
    util.add(msg3);
    util.add(msg4);

    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    expected.add(msg2);
    expected.add(msg3);
    expected.add(msg4);
    expected.add(msg5);
    assertEquals(expected, received);
  }

  @Test
  void handlesUnchainedMessagesInTheOrderInWhichTheyArriveIfTheyAreNewer() {
    final MessageId messageId2 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(4)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage m2 =
        new StreamMessage.Builder()
            .withMessageId(messageId2)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(17)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage m3 =
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(7)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage m4 =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    List<StreamMessage> received = new ArrayList<>();
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            null,
            5000L,
            5000L,
            false);

    util.add(msg1);
    util.add(m2);
    util.add(m3);
    util.add(m4);

    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    expected.add(m2);
    expected.add(m3);
    assertEquals(expected, received);
  }

  @Test
  void doesNotCallTheGapHandler_scheduledButResolvedBeforeTimeout() throws InterruptedException {
    List<StreamMessage> received = new ArrayList<>();
    final RuntimeException[] unexpected = new RuntimeException[1];
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            new OrderedMsgChain.GapHandlerFunction() {
              @Override
              public void apply(
                  MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                unexpected[0] = new RuntimeException("Unexpected gap fill request");
              }
            },
            300L,
            300L,
            false);

    util.add(msg1);
    util.add(msg5);
    Thread.sleep(100L);
    util.add(msg4);
    util.add(msg3);
    util.add(msg2);

    assertNull(unexpected[0]);

    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    expected.add(msg2);
    expected.add(msg3);
    expected.add(msg4);
    expected.add(msg5);
    assertEquals(expected, received);
  }

  @Test
  void callTheGapHandlerMAX_GAP_REQUESTStimesAndThenThrows() throws InterruptedException {
    List<StreamMessage> received = new ArrayList<>();
    final int[] gapHandlerCount = {0};
    OrderedMsgChain util = null;
    final GapFillFailedException[] expected = new GapFillFailedException[1];
    try {
      util =
          new OrderedMsgChain(
              publisherId,
              "msgChainId",
              new Consumer<StreamMessage>() {
                @Override
                public void accept(StreamMessage streamMessage) {
                  received.add(streamMessage);
                }
              },
              new OrderedMsgChain.GapHandlerFunction() {
                @Override
                public void apply(
                    MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                  gapHandlerCount[0]++;
                }
              },
              new Function<GapFillFailedException, Void>() {
                @Override
                public Void apply(GapFillFailedException e) {
                  expected[0] = e;
                  throw e; // mimic behavior of default handler
                }
              },
              100L,
              100L,
              false);
    } catch (GapFillFailedException e) {
      expected[0] = e;
    }

    util.add(msg1);
    util.add(msg3);
    Thread.sleep(2000L);

    assertNotNull(expected[0]);
    assertEquals(10, gapHandlerCount[0]);

    // gap should be cleared
    assertTrue(!util.hasGap());
  }

  @Test
  void handlesUnorderedMessagesInOrder_largeRandomizedTest() {
    List<StreamMessage> expected = new ArrayList<>();
    expected.add(msg1);
    List<StreamMessage> shuffled = new ArrayList<>();
    for (int i = 2; i <= 1000; i++) {
      final MessageId messageId =
          new MessageId.Builder()
              .withStreamId("streamId")
              .withTimestamp(i)
              .withSequenceNumber(0)
              .withPublisherId(TestingAddresses.PUBLISHER_ID)
              .withMsgChainId("msgChainId")
              .createMessageId();
      final byte[] payload = "response".getBytes(StandardCharsets.UTF_8);
      final StreamMessage.Content content =
          StreamMessage.Content.Factory.withJsonAsPayload(payload);
      final StreamMessage msg =
          new StreamMessage.Builder()
              .withMessageId(messageId)
              .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1L, 0L))
              .withContent(content)
              .createStreamMessage();
      expected.add(msg);
      shuffled.add(msg);
    }
    Collections.shuffle(shuffled);
    List<StreamMessage> received = new ArrayList<>();
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received.add(streamMessage);
              }
            },
            new OrderedMsgChain.GapHandlerFunction() {
              @Override
              public void apply(
                  MessageRef from, MessageRef to, Address publisherId, String msgChainId) {}
            },
            5000L,
            5000L,
            false);

    util.add(msg1);
    for (StreamMessage msg : shuffled) {
      util.add(msg);
    }
    boolean receivedAllMessages = received.equals(expected);
    if (!receivedAllMessages) {
      List<Long> receivedTimestamps =
          received.stream()
              .map(
                  new Function<StreamMessage, Long>() {
                    @Override
                    public Long apply(StreamMessage streamMessage) {
                      return streamMessage.getTimestamp();
                    }
                  })
              .collect(Collectors.toList());
      List<Long> shuffledTimestamps =
          shuffled.stream()
              .map(
                  new Function<StreamMessage, Long>() {
                    @Override
                    public Long apply(StreamMessage streamMessage) {
                      return streamMessage.getTimestamp();
                    }
                  })
              .collect(Collectors.toList());
      assertTrue(
          receivedAllMessages,
          "Was expecting to receive messages ordered per timestamp but instead received timestamps in "
              + "this order:\n"
              + receivedTimestamps
              + "\nThe unordered messages were processed "
              + "in the following timestamp order:\n"
              + shuffledTimestamps);
    }

    assertTrue(receivedAllMessages);
  }

  @Test
  void throwsIfTheQueueIsFullIfSkipGapsOnFullQueueIsFalse() {
    final int[] received = {0};
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received[0]++;
              }
            },
            null,
            5000L,
            5000L,
            false);

    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(-1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();

    util.add(
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage());
    // there's a gap between the above and the below messages, so below messages are queued
    for (int i = 1; i <= OrderedMsgChain.MAX_QUEUE_SIZE; i++) {
      final MessageId messageId =
          new MessageId.Builder()
              .withStreamId("streamId")
              .withTimestamp(i)
              .withSequenceNumber(0)
              .withPublisherId(TestingAddresses.PUBLISHER_ID)
              .withMsgChainId("msgChainId")
              .createMessageId();
      StreamMessage streamMessage =
          new StreamMessage.Builder()
              .withMessageId(messageId)
              .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1L, 0L))
              .withContent(TestingContent.emptyMessage())
              .createStreamMessage();
      util.add(streamMessage);
    }
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(OrderedMsgChain.MAX_QUEUE_SIZE + 1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage streamMessage =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(
                TestingMessageRef.createMessageRef((long) OrderedMsgChain.MAX_QUEUE_SIZE, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage();
    assertThrows(
        IllegalStateException.class,
        () -> {
          util.add(streamMessage);
        });
  }

  @Test
  void emptiesTheQueueIfFullIfSkipGapsOnFullQueueIsTrue() {
    final int[] received = {0};
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received[0]++;
              }
            },
            null,
            5000L,
            5000L,
            true);

    final MessageId messageId1 =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(-1)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();

    util.add(
        new StreamMessage.Builder()
            .withMessageId(messageId1)
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage());
    // there's a gap between the above and the below messages, so below messages are queued
    for (int i = 1; i <= OrderedMsgChain.MAX_QUEUE_SIZE; i++) {
      final MessageId messageId =
          new MessageId.Builder()
              .withStreamId("streamId")
              .withTimestamp(i)
              .withSequenceNumber(0)
              .withPublisherId(TestingAddresses.PUBLISHER_ID)
              .withMsgChainId("msgChainId")
              .createMessageId();
      util.add(
          new StreamMessage.Builder()
              .withMessageId(messageId)
              .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1L, 0L))
              .withContent(TestingContent.emptyMessage())
              .createStreamMessage());
    }

    assertTrue(util.isQueueFull());

    received[0] = 0;
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withTimestamp(OrderedMsgChain.MAX_QUEUE_SIZE + 100)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    util.add(
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(
                TestingMessageRef.createMessageRef(OrderedMsgChain.MAX_QUEUE_SIZE + 95L, 0L))
            .withContent(TestingContent.emptyMessage())
            .createStreamMessage());

    assertEquals(1, received[0]);
  }

  // Warning: non-deterministic test. If you see flakiness in this test, it may indicate
  // something is wrong in the thread-safety of the class under test.
  @Test
  void handlesInputFromMultipleThreadsCorrectly() {
    final int[] received = {0};
    OrderedMsgChain.GapHandlerFunction gapHandler =
        new OrderedMsgChain.GapHandlerFunction() {
          @Override
          public void apply(
              final MessageRef from,
              final MessageRef to,
              final Address publisherId,
              final String msgChainId) {
            throw new RuntimeException("GapHandlerFunction.apply(...) should not be called!");
          }
        };
    OrderedMsgChain util =
        new OrderedMsgChain(
            publisherId,
            "msgChainId",
            new Consumer<StreamMessage>() {
              @Override
              public void accept(StreamMessage streamMessage) {
                received[0]++;
              }
            },
            gapHandler,
            5000L,
            5000L,
            false);
    Runnable produce =
        new Runnable() {
          @Override
          public void run() {
            for (int i = 0; i < 1000; i++) {
              final MessageId messageId =
                  new MessageId.Builder()
                      .withStreamId("streamId")
                      .withTimestamp(i)
                      .withSequenceNumber(0)
                      .withPublisherId(TestingAddresses.PUBLISHER_ID)
                      .withMsgChainId("msgChainId")
                      .createMessageId();
              util.add(
                  new StreamMessage.Builder()
                      .withMessageId(messageId)
                      .withPreviousMessageRef(
                          TestingMessageRef.createMessageRef((i == 0L ? null : i - 1L), 0L))
                      .withContent(TestingContent.emptyMessage())
                      .createStreamMessage());
            }
          }
        };
    // Start 2 threads that produce the same messages in parallel
    int expected = 1000;
    Thread a = new Thread(produce);
    Thread b = new Thread(produce);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (expected == received[0]) {
                    break;
                  }
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
                try {
                  a.join();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                try {
                  b.join();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            });
    a.start();
    b.start();
    pollingCondition.start();
    try {
      pollingCondition.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    assertEquals(expected, received[0]);
  }
}
