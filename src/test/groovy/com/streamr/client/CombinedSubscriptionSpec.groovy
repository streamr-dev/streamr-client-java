package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.options.ResendLastOption
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.OrderedMsgChain
import spock.lang.Specification
import com.streamr.client.subs.CombinedSubscription

class CombinedSubscriptionSpec extends Specification {
    StreamMessage createMessage(long timestamp, long sequenceNumber, Long previousTimestamp, Long previousSequenceNumber) {
        return new StreamMessageV31("stream-id", 0, timestamp, sequenceNumber, "publisherId", "msgChainId",
                previousTimestamp, previousSequenceNumber, StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
    }
    void "calls the gap handler if gap among real time messages queued during resend"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        CombinedSubscription sub = new CombinedSubscription("stream-id", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {

            }
        }, new ResendLastOption(10), new HashMap<String, String>(), null, 10L, 10L, false)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
