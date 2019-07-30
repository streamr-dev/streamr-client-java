package com.streamr.client.utils

import com.streamr.client.exceptions.GapFillFailedException
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageV31;
import spock.lang.Specification

import java.util.function.Function
import java.util.stream.Collectors;

class OrderedMsgChainSpec extends Specification {
    StreamMessage createMessage(long timestamp, Long previousTimestamp) {
        return new StreamMessageV31("stream-id", 0, timestamp, 0L, "publisherId", "msgChainId",
                previousTimestamp, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
    }
    StreamMessage msg1 = createMessage(1, null)
    StreamMessage msg2 = createMessage(2, 1)
    StreamMessage msg3 = createMessage(3, 2)
    StreamMessage msg4 = createMessage(4, 3)
    StreamMessage msg5 = createMessage(5, 4)
    void "handles ordered messages in order"() {
        ArrayList<StreamMessage> received = []
        OrderedMsgChain util = new OrderedMsgChain("publisherId", "msgChainId",
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, null, 5000L)
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
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, null, 5000L)
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
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {

            }
        }, 5000L)
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
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, null, 5000L)
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
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
                unexpected = new RuntimeException("Unexpected gap fill request")
            }
        }, 300L)
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
                    new Function<StreamMessage, Void>() {
                        @Override
                        Void apply(StreamMessage streamMessage) {
                            received.add(streamMessage)
                            return null
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
                    return null
                }
            }, 100L)
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
                new Function<StreamMessage, Void>() {
                    @Override
                    Void apply(StreamMessage streamMessage) {
                        received.add(streamMessage)
                        return null
                    }
                }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {

            }
        }, 5000L)
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
}
