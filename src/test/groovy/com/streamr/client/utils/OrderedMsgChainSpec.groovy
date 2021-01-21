package com.streamr.client.utils

import com.streamr.client.exceptions.GapFillFailedException
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamrSpecification
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import spock.util.concurrent.PollingConditions

class OrderedMsgChainSpec extends StreamrSpecification {

    final StreamMessage msg1 = createMessage(1, 0)
    final StreamMessage msg2 = createMessage(2, 0, 1, 0)
    final StreamMessage msg3 = createMessage(3, 0, 2, 0)
    final StreamMessage msg4 = createMessage(4, 0, 3, 0)
    final StreamMessage msg5 = createMessage(5, 0, 4, 0)

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
        StreamMessage m2 = createMessage(4)
        StreamMessage m3 = createMessage(17)
        StreamMessage m4 = createMessage(7)
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
            StreamMessage msg = createMessage(i, 0, i - 1, 0)
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

        when:
        util.add(createMessage(-1))
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i<=OrderedMsgChain.MAX_QUEUE_SIZE + 1; i++) {
            util.add(createMessage(i, 0, i-1, 0))
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

        when:
        util.add(createMessage(-1))
        // there's a gap between the above and the below messages, so below messages are queued
        for (int i=1; i <= OrderedMsgChain.MAX_QUEUE_SIZE; i++) {
            util.add(createMessage(i, 0, i-1, 0))
        }

        assert util.isQueueFull()

        received = 0
        util.add(createMessage(OrderedMsgChain.MAX_QUEUE_SIZE + 100, 0, OrderedMsgChain.MAX_QUEUE_SIZE + 95, 0))

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
                util.add(createMessage(i, 0, (i == 0 ? null : i - 1), 0))
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
