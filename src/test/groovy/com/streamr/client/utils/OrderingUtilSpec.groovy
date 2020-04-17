package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import spock.lang.Specification

import java.util.function.Consumer
import java.util.function.Function

class OrderingUtilSpec extends Specification {
    StreamMessage createMessage(long timestamp, Long previousTimestamp) {
        return new StreamMessageV31("stream-id", 0, timestamp, 0L, "publisherId", "msgChainId",
                previousTimestamp, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null);
    }
    StreamMessage msg1 = createMessage(1, null)
    StreamMessage msg2 = createMessage(2, 1)
    StreamMessage msg3 = createMessage(3, 2)
    StreamMessage msg4 = createMessage(4, 3)
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
        String publisherIdReceived
        String msgChainIdReceived

        OrderingUtil util = new OrderingUtil("streamId", 0, new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {

            }
        }, new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
