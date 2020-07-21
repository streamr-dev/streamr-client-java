package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.BasicSubscription.GroupKeyRequestFunction
import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.Address
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.GroupKeyStore
import com.streamr.client.utils.OrderedMsgChain
import spock.util.concurrent.PollingConditions

/**
 * BasicSubscription is abstract, but contains most of the code for RealtimeSubscription and
 * HistoricalSubscription. This class tests BasicSubscription via RealtimeSubscription.
 */
class BasicSubscriptionSpec extends StreamrSpecification {

    StreamMessage msg
    GroupKeyStore keyStore
    List<StreamMessage> received
    RealTimeSubscription sub

    def setup() {
        msg = createMessage()
        keyStore = Mock(GroupKeyStore)
        received = []
        sub = createSub()
        groupKeyFunctionCallCount = 0
    }

    MessageHandler defaultHandler = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {
            received.add(message)
        }
    }

    int groupKeyFunctionCallCount
    GroupKeyRequestFunction defaultGroupKeyRequestFunction = new BasicSubscription.GroupKeyRequestFunction() {
        @Override
        void apply(Address publisherId, List<String> groupKeyIds) {
            groupKeyFunctionCallCount++
        }
    }

    private RealTimeSubscription createSub(MessageHandler handler = defaultHandler, String streamId = msg.getStreamId(), GroupKeyRequestFunction groupKeyRequestFunction = defaultGroupKeyRequestFunction) {
        return new RealTimeSubscription(streamId, 0, handler, keyStore, groupKeyRequestFunction)
    }

    void "calls the message handler when realtime messages are received"() {
        when:
        sub.handleRealTimeMessage(msg)

        then:
        received[0] == msg
    }

    void "calls the handler once for each message in order"() {
        ArrayList<StreamMessage> msgs = new ArrayList<>()
        for (int i=0;i<5;i++) {
            msgs.add(createMessage((long)i, 0, null, 0))
        }

        when:
        for (int i=0;i<5;i++) {
            sub.handleRealTimeMessage(msgs.get(i))
        }

        then:
        for (int i=0;i<5;i++) {
            assert msgs.get(i).serialize() == received.get(i).serialize()
        }
    }

    void "handles resent messages during resending"() {
        when:
        sub.setResending(true)
        sub.handleResentMessage(msg)

        then:
        received[0] == msg
    }

    void "ignores duplicate messages"() {
        when:
        sub.handleRealTimeMessage(msg)
        sub.handleRealTimeMessage(msg)

        then:
        received.size() == 1
    }

    void "calls the gap handler if a gap is detected"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })

        when:
        sub.handleRealTimeMessage(msg1)
        then:
        ex == null

        when:
        sub.handleRealTimeMessage(msg4)
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

    void "does not throw if different publishers"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0, "publisher1")
        StreamMessage msg4 = createMessage(4, 0, 3, 0, "publisher2")

        when:
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg4)

        then:
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected (same timestamp but different sequence numbers)"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(1, 4, 1, 3)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })

        when:
        sub.handleRealTimeMessage(msg1)
        then:
        ex == null

        when:
        sub.handleRealTimeMessage(msg4)
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

    void "does not throw if there is no gap"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg2 = createMessage(1, 1, 1, 0)
        StreamMessage msg3 = createMessage(4, 0, 1, 1)

        when:
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg2)
        sub.handleRealTimeMessage(msg3)

        then:
        received.size() == 3
        noExceptionThrown()
    }

    void "decrypts encrypted messages with the correct key"() {
        GroupKey groupKey = GroupKey.generate()

        Map plaintext = [foo: 'bar']
        StreamMessage msg1 = createMessage(plaintext)

        EncryptionUtil.encryptStreamMessage(msg1, groupKey)
        keyStore.add(msg1.getStreamId(), groupKey)

        when:
        sub.handleRealTimeMessage(msg1)

        then:
        received[0].getParsedContent() == plaintext
    }

    void "calls key request function when cannot decrypt messages with wrong key (multiple times when no response)"() {
        GroupKey groupKey = GroupKey.generate()
        GroupKey wrongGroupKey = GroupKey.generate()

        EncryptionUtil.encryptStreamMessage(msg, groupKey)
        keyStore.add(msg.getStreamId(), wrongGroupKey)

        String receivedPublisherId = null
        int nbCalls = 0
        int timeout = 3000
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, defaultHandler, keyStore,
            new GroupKeyRequestFunction() {
                @Override
                void apply(Address publisherId, List<String> groupKeyIds) {
                    receivedPublisherId = publisherId
                    nbCalls++
                }
            }, timeout, 5000, false)

        when:
        // First call to groupKeyRequestFunction
        sub.handleRealTimeMessage(msg)
        // Wait for 2 timeouts to happen
        Thread.sleep(timeout * 2 + 1500)
        then:
        nbCalls == 3

        when:
        sub.onNewKeysAdded(msg.getPublisherId(), [groupKey])
        Thread.sleep(timeout * 2)
        then:
        receivedPublisherId == msg.getPublisherId().toLowerCase()
        nbCalls == 3
    }

    void "calls key request function MAX_NB_GROUP_KEY_REQUESTS times"() {
        GroupKey groupKey = GroupKey.generate()
        EncryptionUtil.encryptStreamMessage(msg, groupKey)

        int nbCalls = 0
        int timeout = 200
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, defaultHandler, keyStore,
                new GroupKeyRequestFunction() {
                    @Override
                    void apply(Address publisherId, List<String> groupKeyIds) {
                        nbCalls++
                    }
                }, timeout, 5000, false)

        when:
        sub.handleRealTimeMessage(msg)
        Thread.sleep(timeout * (BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS + 2))

        then:
        nbCalls == BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS
    }

    void "queues messages when not able to decrypt and handles them once the key is updated"() {
        StreamMessage msg1 = createMessage(1, [foo: 'bar1'])
        StreamMessage msg2 = createMessage(2, [foo: 'bar2'])

        GroupKey groupKey = GroupKey.generate()
        EncryptionUtil.encryptStreamMessage(msg1, groupKey)
        EncryptionUtil.encryptStreamMessage(msg2, groupKey)

        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleRealTimeMessage(msg1)

        then:
        new PollingConditions().within(10) {
            groupKeyFunctionCallCount == 1
        }

        when:
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2)

        then:
        groupKeyFunctionCallCount == 1

        // faking the reception of the group key response
        when:
        sub.onNewKeysAdded(msg1.getPublisherId(), [groupKey])

        then:
        received.size() == 2
        received[0].getParsedContent() == [foo: 'bar1']
        received[1].getParsedContent() == [foo: 'bar2']
        groupKeyFunctionCallCount == 1
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, "publisherId1", [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, "publisherId1", [foo: 'bar2'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, "publisherId2", [foo: 'bar3'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, "publisherId2", [foo: 'bar4'])

        GroupKey groupKey1 = GroupKey.generate()
        GroupKey groupKey2 = GroupKey.generate()

        EncryptionUtil.encryptStreamMessage(msg1pub1, groupKey1)
        EncryptionUtil.encryptStreamMessage(msg2pub1, groupKey1)
        EncryptionUtil.encryptStreamMessage(msg1pub2, groupKey2)
        EncryptionUtil.encryptStreamMessage(msg2pub2, groupKey2)

        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleRealTimeMessage(msg1pub1)
        then:
        new PollingConditions().within(10) {
            groupKeyFunctionCallCount == 1
        }

        when:
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2pub1)
        then:
        groupKeyFunctionCallCount == 1

        when:
        // Cannot decrypt msg3, queues it and calls the handler
        sub.handleRealTimeMessage(msg1pub2)
        then:
        new PollingConditions().within(10) {
            groupKeyFunctionCallCount == 2
        }

        when:
        // Cannot decrypt msg4, queues it.
        sub.handleRealTimeMessage(msg2pub2)
        then:
        groupKeyFunctionCallCount == 2

        when:
        // faking the reception of the group key response
        sub.onNewKeysAdded(msg1pub1.getPublisherId(), [groupKey1])
        sub.onNewKeysAdded(msg1pub2.getPublisherId(), [groupKey2])
        then:
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']
        received.get(2).getParsedContent() == [foo: 'bar3']
        received.get(3).getParsedContent() == [foo: 'bar4']
        groupKeyFunctionCallCount == 2
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers interleaved)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, "publisherId1", [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, "publisherId1", [foo: 'bar2'])
        StreamMessage msg3pub1 = createMessage(3, 0, null, null, "publisherId1", [foo: 'bar3'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, "publisherId2", [foo: 'bar4'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, "publisherId2", [foo: 'bar5'])

        GroupKey groupKey1 = GroupKey.generate()
        GroupKey groupKey2 = GroupKey.generate()

        EncryptionUtil.encryptStreamMessage(msg1pub1, groupKey1)
        EncryptionUtil.encryptStreamMessage(msg2pub1, groupKey1)
        EncryptionUtil.encryptStreamMessage(msg1pub2, groupKey2)
        EncryptionUtil.encryptStreamMessage(msg2pub2, groupKey2)

        when:
        sub.handleRealTimeMessage(msg1pub1)
        sub.handleRealTimeMessage(msg1pub2)
        then:
        new PollingConditions().within(10) {
            groupKeyFunctionCallCount == 2
        }

        when:
        sub.handleRealTimeMessage(msg2pub1)
        sub.onNewKeysAdded("publisherId1", [groupKey1])
        sub.handleRealTimeMessage(msg3pub1)
        sub.handleRealTimeMessage(msg2pub2)
        sub.onNewKeysAdded("publisherId2", [groupKey2])

        then:
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']
        received.get(2).getParsedContent() == [foo: 'bar3']
        received.get(3).getParsedContent() == [foo: 'bar4']
        received.get(4).getParsedContent() == [foo: 'bar5']
        groupKeyFunctionCallCount == 2
    }

    void "throws when not able to decrypt after receiving key"() {
        GroupKey correctGroupKey = GroupKey.generate()
        GroupKey incorrectGroupKeyWithCorrectId = new GroupKey(correctGroupKey.getGroupKeyId(), GroupKey.generate().getGroupKeyHex())

        EncryptionUtil.encryptStreamMessage(msg, correctGroupKey)

        when:
        sub.handleRealTimeMessage(msg) // queues message
        sub.onNewKeysAdded(msg.getPublisherId(), [incorrectGroupKeyWithCorrectId])

        then:
        thrown(UnableToDecryptException)
    }

    // TODO: good test?
    void "doesn't throw when some other key is added (?)"() {
        GroupKey correctGroupKey = GroupKey.generate()
        GroupKey otherKey = GroupKey.generate()

        EncryptionUtil.encryptStreamMessage(msg, correctGroupKey)

        when:
        sub.handleRealTimeMessage(msg) // queues message
        sub.onNewKeysAdded(msg.getPublisherId(), [otherKey]) // other key added (not waiting for this key)

        then:
        noExceptionThrown()
        //thrown(UnableToDecryptException)
    }

}
