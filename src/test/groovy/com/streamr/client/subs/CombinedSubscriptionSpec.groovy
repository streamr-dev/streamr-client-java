package com.streamr.client.subs

import com.streamr.client.MessageHandler
import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.options.ResendLastOption
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.testing.TestingMessageRef
import com.streamr.client.utils.Address
import com.streamr.client.utils.GroupKeyStore
import com.streamr.client.utils.KeyExchangeUtil
import com.streamr.client.utils.OrderedMsgChain
import spock.lang.Specification

class CombinedSubscriptionSpec extends Specification {

    void "calls the gap handler if gap among real time messages queued during resend"() {
		final MessageId messageId2 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(1)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChainId")
				.createMessageId()
		StreamMessage msg1 = new StreamMessage.Builder()
				.withMessageId(messageId2)
				.withContent(TestingContent.emptyMessage())
				.createStreamMessage()
		final MessageId messageId1 = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(1)
				.withSequenceNumber(1)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChainId")
				.createMessageId()
		StreamMessage afterMsg1 = new StreamMessage.Builder()
				.withMessageId(messageId1)
				.withContent(TestingContent.emptyMessage())
				.createStreamMessage()
		final MessageId messageId = new MessageId.Builder()
				.withStreamId("streamId")
				.withTimestamp(4)
				.withSequenceNumber(0)
				.withPublisherId(TestingAddresses.PUBLISHER_ID)
				.withMsgChainId("msgChainId")
				.createMessageId()
		StreamMessage msg4 = new StreamMessage.Builder()
				.withMessageId(messageId)
				.withPreviousMessageRef(TestingMessageRef.createMessageRef(3, 0))
				.withContent(TestingContent.emptyMessage())
				.createStreamMessage()
		CombinedSubscription sub = new CombinedSubscription(msg1.getStreamId(), 0, new MessageHandler() {
			@Override
			void onMessage(Subscription sub, StreamMessage message) {

			}
		}, Mock(GroupKeyStore), Mock(KeyExchangeUtil), new ResendLastOption(10), null, 10L, 10L, false)
		GapDetectedException ex
		sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
			@Override
			void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
				ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
			}
		})
		when:
		sub.handleResentMessage(msg1)
		sub.handleRealTimeMessage(msg4)
		sub.endResend()
		Thread.sleep(50L)
		sub.clear()
		then:
		ex.getStreamId() == msg1.getStreamId()
		ex.getStreamPartition() == msg1.getStreamPartition()
		ex.getFrom() == afterMsg1.getMessageRef()
		ex.getTo() == msg4.getPreviousMessageRef()
		ex.getPublisherId() == msg1.getPublisherId()
		ex.getMsgChainId() == msg1.getMsgChainId()
	}
}
