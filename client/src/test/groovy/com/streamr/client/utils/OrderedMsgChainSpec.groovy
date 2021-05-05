package com.streamr.client.utils

import com.streamr.client.exceptions.GapFillFailedException
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.testing.TestingMessageRef
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class OrderedMsgChainSpec extends Specification {
    final StreamMessage.Content content = TestingContent.emptyMessage()
    final StreamMessage msg1 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(1).withSequenceNumber(0).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg2 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(2).withSequenceNumber(0).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(1, 0))
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg3 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(3).withSequenceNumber(0).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(2, 0))
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg4 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(4).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(3, 0))
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg5 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(5).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(4, 0))
            .withContent(content)
            .createStreamMessage()

    final Address publisherId = new Address("0x12345")

    void "handles ordered messages in order"() {
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received.add(streamMessage)
            }
        }, null, 5000L, 5000L, false)
        when:
        util.add(msg1)
        util.add(msg2)
        util.add(msg3)
        then:
        received == [msg1, msg2, msg3]
    }
    void "drops duplicates"() {
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, null, 5000L, 5000L, false)
        when:
        util.add(msg1)
        util.add(msg1)
        util.add(msg2)
        util.add(msg1)
        then:
        received == [msg1, msg2]
    }
    void "handles unordered messages in order"() {
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {

            }
        }, 5000L, 5000L, false)
        when:
        util.add(msg1)
        util.add(msg2)
        util.add(msg5)
        util.add(msg3)
        util.add(msg4)
        then:
        received == [msg1, msg2, msg3, msg4, msg5]
    }
    void "handles unchained messages in the order in which they arrive if they are newer"() {
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(4)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage m2 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(17)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage m3 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(7)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage m4 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, null, 5000L, 5000L, false)
        when:
        util.add(msg1)
        util.add(m2)
        util.add(m3)
        util.add(m4)
        then:
        received == [msg1, m2, m3]
    }
    void "does not call the gap handler (scheduled but resolved before timeout)"() {
        ArrayList<StreamMessage> received = []
        RuntimeException unexpected
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                unexpected = new RuntimeException("Unexpected gap fill request")
            }
        }, 300L, 300L, false)
        when:
        util.add(msg1)
        util.add(msg5)
        Thread.sleep(100L)
        util.add(msg4)
        util.add(msg3)
        util.add(msg2)
        then:
        unexpected == null
        received == [msg1, msg2, msg3, msg4, msg5]
    }
    void "call the gap handler MAX_GAP_REQUESTS times and then throws"() {
        ArrayList<StreamMessage> received = []
        int gapHandlerCount = 0
        OrderedMsgChain util
        GapFillFailedException expected
        try {
            util = new OrderedMsgChain(publisherId, "msgChainId",
                    new Consumer<StreamMessage>() {
                        @Override
                        void accept(StreamMessage streamMessage) {
                            received.add(streamMessage)
                        }
                    }, new OrderedMsgChain.GapHandlerFunction() {
                @Override
                void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                    gapHandlerCount++
                }
            }, new Function<GapFillFailedException, Void>() {
                @Override
                Void apply(GapFillFailedException e) {
                    expected = e
                    throw e // mimic behavior of default handler
                }
            }, 100L, 100L, false)
        } catch (GapFillFailedException e) {
            expected = e
        }
        when:
        util.add(msg1)
        util.add(msg3)
        Thread.sleep(1200L)

        then:
        expected != null
        gapHandlerCount == 10

        then: "gap should be cleared"
        !util.hasGap()
    }

    void "handles unordered messages in order (large randomized test)"() {
        ArrayList<StreamMessage> expected = [msg1]
        ArrayList<StreamMessage> shuffled = []
        for (int i = 2; i <= 1000; i++) {
            final MessageId messageId = new MessageId.Builder()
                    .withStreamId("streamId")
                    .withTimestamp(i)
                    .withSequenceNumber(0)
                    .withPublisherId(TestingAddresses.PUBLISHER_ID)
                    .withMsgChainId("msgChainId")
                    .createMessageId()
            final byte[] payload = "response".getBytes(StandardCharsets.UTF_8);
            final StreamMessage.Content content = StreamMessage.Content.Factory.withJsonAsPayload(payload);
            final StreamMessage msg = new StreamMessage.Builder()
                    .withMessageId(messageId)
                    .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1, 0))
                    .withContent(content)
                    .createStreamMessage()
            expected.add(msg)
            shuffled.add(msg)
        }
        Collections.shuffle(shuffled)
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {

            }
        }, 5000L, 5000L, false)
        when:
        util.add(msg1)
        for (StreamMessage msg: shuffled) {
            util.add(msg)
        }
        boolean result = received == expected
        if (!result) {
            List<Long> receivedTimestamps = received.stream().map(new Function<StreamMessage, Long>() {
                @Override
                Long apply(StreamMessage streamMessage) {
                    return streamMessage.getTimestamp()
                }
            }).collect(Collectors.toList())
            List<Long> shuffledTimestamps = shuffled.stream().map(new Function<StreamMessage, Long>() {
                @Override
                Long apply(StreamMessage streamMessage) {
                    return streamMessage.getTimestamp()
                }
            }).collect(Collectors.toList())
            assert result, "Was expecting to receive messages ordered per timestamp but instead received timestamps in " +
                    "this order:\n" + receivedTimestamps.join(", ") + "\nThe unordered messages were processed" +
                    "in the following timestamp order:\n" + shuffledTimestamps.join(", ")
        }
        then:
        result
    }

    void "throws if the queue is full if skipGapsOnFullQueue is false"() {
        int received = 0;
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++;
            }
        }, null, 5000L, 5000L, false)

        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(-1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        when:
        util.add(new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage())
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i<=OrderedMsgChain.MAX_QUEUE_SIZE + 1; i++) {
            final MessageId messageId = new MessageId.Builder()
                    .withStreamId("streamId")
                    .withTimestamp(i)
                    .withSequenceNumber(0)
                    .withPublisherId(TestingAddresses.PUBLISHER_ID)
                    .withMsgChainId("msgChainId")
                    .createMessageId()
            util.add(new StreamMessage.Builder()
                    .withMessageId(messageId)
                    .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1, 0))
                    .withContent(TestingContent.emptyMessage())
                    .createStreamMessage())
        }

        then:
        thrown(IllegalStateException)
    }

    void "empties the queue if full if skipGapsOnFullQueue is true"() {
        int received = 0
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++
            }
        }, null, 5000L, 5000L, true)

        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(-1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        when:
        util.add(new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage())
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i <= OrderedMsgChain.MAX_QUEUE_SIZE; i++) {
            final MessageId messageId = new MessageId.Builder()
                    .withStreamId("streamId")
                    .withTimestamp(i)
                    .withSequenceNumber(0)
                    .withPublisherId(TestingAddresses.PUBLISHER_ID)
                    .withMsgChainId("msgChainId")
                    .createMessageId()
            util.add(new StreamMessage.Builder()
                    .withMessageId(messageId)
                    .withPreviousMessageRef(TestingMessageRef.createMessageRef(i - 1, 0))
                    .withContent(TestingContent.emptyMessage())
                    .createStreamMessage())
        }

        assert util.isQueueFull()

        received = 0
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(OrderedMsgChain.MAX_QUEUE_SIZE + 100)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        util.add(new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(OrderedMsgChain.MAX_QUEUE_SIZE + 95, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage())

        then:
        received == 1
    }

    // Warning: non-deterministic test. If you see flakiness in this test, it may indicate
    // something is wrong in the thread-safety of the class under test.
    void "handles input from multiple threads correctly"() {
        int received = 0
        OrderedMsgChain.GapHandlerFunction gapHandler = Mock(OrderedMsgChain.GapHandlerFunction)
        OrderedMsgChain util = new OrderedMsgChain(publisherId, "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++
            }
        }, gapHandler, 5000L, 5000L, false)
        when:
        Closure produce = {
            for (int i=0; i<1000; i++) {
                final MessageId messageId = new MessageId.Builder()
                        .withStreamId("streamId")
                        .withTimestamp(i)
                        .withSequenceNumber(0)
                        .withPublisherId(TestingAddresses.PUBLISHER_ID)
                        .withMsgChainId("msgChainId")
                        .createMessageId()
                util.add(new StreamMessage.Builder()
                        .withMessageId(messageId)
                        .withPreviousMessageRef(TestingMessageRef.createMessageRef((i == 0 ? null : i - 1), 0))
                        .withContent(TestingContent.emptyMessage())
                        .createStreamMessage())
            }
        }
        // Start 2 threads that produce the same messages in parallel
        Thread.start(produce)
        Thread.start(produce)

        then:
        new PollingConditions(timeout: 10).eventually {
            received == 1000
        }
        0 * gapHandler.apply(_, _, _, _)
    }

}
