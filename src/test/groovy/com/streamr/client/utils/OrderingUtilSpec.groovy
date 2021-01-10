package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamrSpecification
import java.util.function.Consumer

class OrderingUtilSpec extends StreamrSpecification {

    StreamMessage msg1 = createMessage(1, 0)
    StreamMessage msg2 = createMessage(2, 0, 1, 0)
    StreamMessage msg3 = createMessage(3, 0, 2, 0)
    StreamMessage msg4 = createMessage(4, 0, 3, 0)

    void "calls the message handler when a message is received"() {
        StreamMessage received
        OrderingUtil util = new OrderingUtil("streamId", 0, new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                received = streamMessage
            }
        }, null, 5000L, 5000L, false)
        when:
        util.add(msg1)
        then:
        received == msg1
    }
    void "calls the gap handler when a gap is detected"() {
        MessageRef fromReceived
        MessageRef toReceived
        Address publisherIdReceived
        String msgChainIdReceived

        OrderingUtil util = new OrderingUtil("streamId", 0, new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {

            }
        }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                fromReceived = from
                toReceived = to
                publisherIdReceived = publisherId
                msgChainIdReceived = msgChainId
            }
        }, 100L, 100L, false)
        when:
        util.add(msg1)
        util.add(msg4)
        Thread.sleep(150L)
        util.add(msg2)
        util.add(msg3)
        then:
        fromReceived.getTimestamp() == 1L
        fromReceived.getSequenceNumber() == 1L
        toReceived.getTimestamp() == 3L
        toReceived.getSequenceNumber() == 0L
        publisherIdReceived == msg1.getPublisherId()
        msgChainIdReceived == msg1.getMsgChainId()
    }
    void "does not call gap handler when gap detected but resolved before request should be sent"() {
        boolean called = false

        OrderingUtil util = new OrderingUtil("streamId", 0, new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {

            }
        }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                called = true
            }
        }, 1000L, 1000L, false)
        when:
        util.add(msg1)
        util.add(msg4)
        util.add(msg2)
        util.add(msg3)
        Thread.sleep(1200L)
        then:
        !called
    }
}
