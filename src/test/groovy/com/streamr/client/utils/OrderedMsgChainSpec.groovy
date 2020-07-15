package com.streamr.client.utils

import com.streamr.client.exceptions.GapFillFailedException
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors;

class OrderedMsgChainSpec extends Specification {
    StreamMessage createMessage(long timestamp, Long previousTimestamp) {
        return new StreamMessage(
                new MessageID("stream-id", 0, timestamp, 0L, "publisherId", "msgChainId"),
                new MessageRef(previousTimestamp, 0L),
                [:])
    }
    StreamMessage msg1 = createMessage(1, null)
    StreamMessage msg2 = createMessage(2, 1)
    StreamMessage msg3 = createMessage(3, 2)
    StreamMessage msg4 = createMessage(4, 3)
    StreamMessage msg5 = createMessage(5, 4)
    void "handles ordered messages in order"() {
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId", new Consumer<StreamMessage>() {
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
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
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
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {

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
        StreamMessage m2 = createMessage(4, null)
        StreamMessage m3 = createMessage(17, null)
        StreamMessage m4 = createMessage(7, null)
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
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
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
            util = new OrderedMsgChain("publisherId", "msgChainId",
                    new Consumer<StreamMessage>() {
                        @Override
                        void accept(StreamMessage streamMessage) {
                            received.add(streamMessage)
                        }
                    }, new OrderedMsgChain.GapHandlerFunction() {
                @Override
                void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
            StreamMessage msg = createMessage(i, i - 1)
            expected.add(msg)
            shuffled.add(msg)
        }
        Collections.shuffle(shuffled)
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
                new Consumer<StreamMessage>() {
                    @Override
                    void accept(StreamMessage streamMessage) {
                        received.add(streamMessage)
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {

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
            println("Was expecting to receive messages ordered per timestamp but instead received timestamps in " +
                    "this order:\n" + receivedTimestamps.join(", ") + "\nThe unordered messages were processed" +
                    "in the following timestamp order:\n" + shuffledTimestamps.join(", "))
        }
        then:
        result
    }

    void "throws if the queue is full if skipGapsOnFullQueue is false"() {
        final int received = 0;
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++;
            }
        }, null, 5000L, 5000L, false)

        when:
        util.add(createMessage(-1, null))
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i<=OrderedMsgChain.MAX_QUEUE_SIZE + 1; i++) {
            util.add(createMessage(i, i-1))
        }

        then:
        thrown(IllegalStateException)
    }

    void "empties the queue if full if skipGapsOnFullQueue is true"() {
        int received = 0
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++
            }
        }, null, 5000L, 5000L, true)

        when:
        util.add(createMessage(-1, null))
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i <= OrderedMsgChain.MAX_QUEUE_SIZE; i++) {
            util.add(createMessage(i, i-1))
        }

        assert util.isQueueFull()

        received = 0
        util.add(createMessage(OrderedMsgChain.MAX_QUEUE_SIZE + 100, OrderedMsgChain.MAX_QUEUE_SIZE + 95))

        then:
        received == 1
    }

    // Warning: non-deterministic test. If you see flakiness in this test, it may indicate
    // something is wrong in the thread-safety of the class under test.
    void "handles input from multiple threads correctly"() {
        int received = 0
        OrderedMsgChain.GapHandlerFunction gapHandler = Mock(OrderedMsgChain.GapHandlerFunction)
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId", new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received++
            }
        }, gapHandler, 5000L, 5000L, false)
        when:
        Closure produce = {
            for (int i=0; i<1000; i++) {
                util.add(createMessage(i, (i == 0 ? null : i - 1)))
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
