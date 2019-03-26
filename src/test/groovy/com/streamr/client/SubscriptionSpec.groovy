package com.streamr.client

import com.streamr.client.options.ResendFromOption
import com.streamr.client.options.ResendLastOption
import com.streamr.client.protocol.control_layer.ResendRangeRequest
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import spock.lang.Specification

class SubscriptionSpec extends Specification {

    StreamMessageV30 msg = new StreamMessageV30("stream-id", 0, (new Date()).getTime(), 0, "publisherId", "msgChainId",
            null, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

    boolean done = false
    void setup() {
        done = false
    }

    StreamMessage createMessage(long timestamp, long sequenceNumber, Long previousTimestamp, Long previousSequenceNumber) {
        return createMessage(timestamp, sequenceNumber, previousTimestamp, previousSequenceNumber, "publisherId")
    }

    StreamMessage createMessage(long timestamp, long sequenceNumber, Long previousTimestamp, Long previousSequenceNumber, String publisherId) {
        return new StreamMessageV30("stream-id", 0, timestamp, sequenceNumber, publisherId, "msgChainId",
                previousTimestamp, previousSequenceNumber, StreamMessage.ContentType.CONTENT_TYPE_JSON, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
    }

    MessageHandler empty = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {

        }
    }

    void "calls the message handler"() {
        when:
        Subscription sub = new Subscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                assert msg.toJson() == message.toJson()
                done = true
            }
        })
        sub.handleMessage(msg, 'session-token')
        then:
        done
    }

    void "calls the handler once for each message in order"() {
        ArrayList<StreamMessage> msgs = new ArrayList<>()
        for (int i=0;i<5;i++) {
            msgs.add(createMessage((long)i, 0, null, 0))
        }
        ArrayList<StreamMessage> received = new ArrayList<>()
        int counter = 0
        when:
        Subscription sub = new Subscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                assert msgs.get(counter).toJson() == message.toJson()
                received.add(message)
                counter++
                if (counter == 5) {
                    done = true
                }
            }
        })
        for (int i=0;i<5;i++) {
            sub.handleMessage(msgs.get(i), 'session-token')
        }
        then:
        done
    }

    void "does not handle messages (queued) during resending"() {
        when:
        Subscription sub = new Subscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                throw new Exception("Shouldn't handle this message!")
            }
        })
        sub.setResending(true)
        sub.handleMessage(msg, 'session-token')
        then:
        assert true
    }

    void "handles messages during resending if isResend=true"() {
        when:
        Subscription sub = new Subscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                assert msg.toJson() == message.toJson()
                done = true
            }
        })
        sub.setResending(true)
        sub.handleMessage(msg, 'session-token', true)
        then:
        done
    }

    void "ignores duplicate messages"() {
        int counter = 0
        when:
        Subscription sub = new Subscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                assert msg.toJson() == message.toJson()
                counter++
                done = true
                if (counter == 2) {
                    throw new Exception("Shouldn't handle this duplicate message!")
                }
            }
        })
        sub.handleMessage(msg, 'session-token')
        sub.handleMessage(msg, 'session-token')
        then:
        done
    }

    void "returns a ResendRangeRequest if a gap is detected"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        ResendRangeRequest req = new ResendRangeRequest(msg1.getStreamId(), msg1.getStreamPartition(), sub.getId(),
                msg1.getMessageRef(), msg4.getPreviousMessageRef(), msg1.getPublisherId(), msg1.getMsgChainId(), 'session-token')
        then:
        sub.handleMessage(msg1, 'session-token') == null
        sub.handleMessage(msg4, 'session-token').toJson() == req.toJson()
    }

    void "returns null if different publishers"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0, "publisher1")
        StreamMessage msg4 = createMessage(4, 0, 3, 0, "publisher2")
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        then:
        sub.handleMessage(msg1, 'session-token') == null
        sub.handleMessage(msg4, 'session-token') == null
    }

    void "returns a ResendRangeRequest if a gap is detected (same timestamp but different sequence numbers)"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg4 = createMessage(1, 4, 1, 3)
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        ResendRangeRequest req = new ResendRangeRequest(msg1.getStreamId(), msg1.getStreamPartition(), sub.getId(),
                msg1.getMessageRef(), msg4.getPreviousMessageRef(), msg1.getPublisherId(), msg1.getMsgChainId(), 'session-token')
        then:
        sub.handleMessage(msg1, 'session-token') == null
        sub.handleMessage(msg4, 'session-token').toJson() == req.toJson()
    }

    void "returns null if there is no gap"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg2 = createMessage(1, 1, 1, 0)
        StreamMessage msg3 = createMessage(4, 0, 1, 1)
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        then:
        sub.handleMessage(msg1, 'session-token') == null
        sub.handleMessage(msg2, 'session-token') == null
        sub.handleMessage(msg3, 'session-token') == null
    }

    void "marks the message as received if an IOException occurs, and continues normally on next message"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg2 = createMessage(1, 1, 1, 0)
        StreamMessage msg3 = createMessage(4, 0, 1, 1)
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        then:
        sub.handleMessage(msg1, 'session-token') == null
        sub.handleError(new IOException(), msg2)
        sub.handleMessage(msg3, 'session-token') == null
    }

    void "if an IOException AND a gap occur, does not mark it as received and returns ResendRangeRequest at the next message"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg3 = createMessage(3, 0, 2, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        when:
        Subscription sub = new Subscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        ResendRangeRequest req = new ResendRangeRequest(msg1.getStreamId(), msg1.getStreamPartition(), sub.getId(),
                msg1.getMessageRef(), msg4.getPreviousMessageRef(), msg1.getPublisherId(), msg1.getMsgChainId(), 'session-token')
        then:
        sub.handleMessage(msg1, 'session-token') == null
        // msg2 is missing
        sub.handleError(new IOException(), msg3)
        sub.handleMessage(msg4, 'session-token').toJson() == req.toJson()
    }

    void "updates the State"() {
        when:
        Subscription sub = new Subscription("streamId", 0, empty)
        sub.setState(Subscription.State.SUBSCRIBED)
        then:
        sub.getState() == Subscription.State.SUBSCRIBED
    }

    void "getEffectiveResendOption() returns original resend option"() {
        when:
        Subscription sub = new Subscription("streamId", 0, empty, new ResendLastOption(10))
        then:
        ((ResendLastOption) sub.getEffectiveResendOption()).getNumberLast() == 10
    }

    void "getEffectiveResendOption() updates the original resend from option after message received"() {
        StreamMessage msg1 = createMessage(10, 0, null, 0)
        ResendFromOption resendFromOption = new ResendFromOption(1, 0, msg1.getPublisherId(), msg1.getMsgChainId())
        when:
        Subscription sub = new Subscription("streamId", 0, empty, resendFromOption)
        sub.handleMessage(msg1, 'session-token')
        ResendFromOption newResendFromOption = (ResendFromOption) sub.getEffectiveResendOption()
        then:
        newResendFromOption.from.timestamp == 10
        newResendFromOption.from.sequenceNumber == 0
        newResendFromOption.publisherId == resendFromOption.publisherId
        newResendFromOption.msgChainId == resendFromOption.msgChainId
    }
}
