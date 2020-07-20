package com.streamr.client

import com.streamr.client.options.ResendLastOption
import com.streamr.client.options.ResendOption
import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.HistoricalSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.GroupKeyStore
import spock.util.concurrent.PollingConditions

/**
 * HistoricalSubscription is only a thin extension of BasicSubscription.
 * Most of the code is already tested in BasicSubscriptionSpec.
 * These tests focus on testing the additional code in HistoricalSubscription.
 */
class HistoricalSubscriptionSpec extends StreamrSpecification {
    StreamMessage msg
    GroupKeyStore keyStore
    List<StreamMessage> received
    HistoricalSubscription sub
    boolean doneHandlerCalled

    def setup() {
        msg = createMessage()
        keyStore = Mock(GroupKeyStore)
        received = []
        sub = createSub()
        groupKeyFunctionCallCount = 0
        doneHandlerCalled = false
    }

    MessageHandler defaultHandler = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {
            received.add(message)
        }

        @Override
        void done(Subscription sub) {
            doneHandlerCalled = true
        }
    }

    int groupKeyFunctionCallCount
    BasicSubscription.GroupKeyRequestFunction defaultGroupKeyRequestFunction = new BasicSubscription.GroupKeyRequestFunction() {
        @Override
        void apply(String publisherId, List<String> groupKeyIds) {
            groupKeyFunctionCallCount++
        }
    }

    private HistoricalSubscription createSub(MessageHandler handler = defaultHandler, ResendOption resendOption = new ResendLastOption(10), String streamId = msg.getStreamId(), BasicSubscription.GroupKeyRequestFunction groupKeyRequestFunction = defaultGroupKeyRequestFunction) {
        return new HistoricalSubscription(streamId, 0, handler, keyStore, resendOption, groupKeyRequestFunction)
    }

    void "does not handle real-time messages (queued)"() {
        when:
        sub.handleRealTimeMessage(msg)

        then:
        noExceptionThrown()
        received.isEmpty()
    }

    void "calls the done handler only when the encryption queue is empty"() {
        GroupKey key = GroupKey.generate()

        HistoricalSubscription sub = createSub()

        EncryptionUtil.encryptStreamMessage(msg, key)

        when:
        sub.handleResentMessage(msg) // queued

        then:
        new PollingConditions().eventually {
            groupKeyFunctionCallCount == 1
        }

        when:
        sub.endResend()

        then:
        !doneHandlerCalled

        when:
        sub.onNewKeys(msg.getPublisherId(), [key])

        then:
        received.size() == 1
        doneHandlerCalled
    }
}
