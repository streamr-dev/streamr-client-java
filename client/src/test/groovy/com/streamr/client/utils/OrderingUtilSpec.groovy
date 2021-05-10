package com.streamr.client.utils

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.testing.TestingMessageRef
import java.util.function.Consumer
import spock.lang.Specification

class OrderingUtilSpec extends Specification {
    final StreamMessage.Content content = TestingContent.emptyMessage()
    final StreamMessage msg1 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(1).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg2 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(2).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(1, 0))
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg3 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(3).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(2, 0))
            .withContent(content)
            .createStreamMessage()
    final StreamMessage msg4 = new StreamMessage.Builder()
            .withMessageId(new MessageId.Builder().withTimestamp(4).withStreamId("streamId").withPublisherId(TestingAddresses.PUBLISHER_ID).withMsgChainId("msgChainId").createMessageId())
            .withPreviousMessageRef(TestingMessageRef.createMessageRef(3, 0))
            .withContent(content)
            .createStreamMessage()

    void "calls the message handler when a message is received"() {
        StreamMessage received
        OrderingUtil util = new OrderingUtil(new Consumer<StreamMessage>() {
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

        OrderingUtil util = new OrderingUtil(new Consumer<StreamMessage>() {
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

        OrderingUtil util = new OrderingUtil(new Consumer<StreamMessage>() {
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
