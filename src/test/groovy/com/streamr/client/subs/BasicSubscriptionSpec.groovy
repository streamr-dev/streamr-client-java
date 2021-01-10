package com.streamr.client.subs

import com.streamr.client.MessageHandler
import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.protocol.message_layer.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription.GroupKeyRequestFunction
import com.streamr.client.utils.Address
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.GroupKeyStore
import com.streamr.client.utils.KeyExchangeUtil
import com.streamr.client.utils.OrderedMsgChain

/**
 * BasicSubscription is abstract, but contains most of the code for RealtimeSubscription and
 * HistoricalSubscription. This class tests BasicSubscription via RealtimeSubscription.
 */
class BasicSubscriptionSpec extends StreamrSpecification {

    private static final long propagationTimeout = 1000
    private static final long resendTimeout = 1000

    StreamMessage msg
    GroupKeyStore keyStore
    KeyExchangeUtil keyExchangeUtil
    List<StreamMessage> received
    RealTimeSubscription sub
    int groupKeyRequestCount
    int unableToDecryptCount

    def setup() {
        msg = createMessage()
        keyStore = Mock(GroupKeyStore)
        keyExchangeUtil = Mock(KeyExchangeUtil)
        received = []
        sub = createSub()
        groupKeyRequestCount = 0
        unableToDecryptCount = 0
    }

    MessageHandler defaultHandler = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {
            received.add(message)
        }

        @Override
        void onUnableToDecrypt(UnableToDecryptException e) {
            unableToDecryptCount++
        }
    }

    GroupKeyRequestFunction defaultGroupKeyRequestFunction = new GroupKeyRequestFunction() {
        @Override
        void apply(Address publisherId, List<String> groupKeyIds) {
            groupKeyRequestCount++
        }
    }

    private RealTimeSubscription createSub(MessageHandler handler = defaultHandler, String streamId = msg.getStreamId(), GroupKeyRequestFunction groupKeyRequestFunction = defaultGroupKeyRequestFunction) {
        return new RealTimeSubscription(streamId, 0, handler, keyStore, keyExchangeUtil, groupKeyRequestFunction, propagationTimeout, resendTimeout, false)
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
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                // GapDetectedException is used just to store the values the gap handler is called with
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })

        when:
        sub.handleRealTimeMessage(msg1)
        then:
        ex == null

        when:
        sub.handleRealTimeMessage(msg4)
        Thread.sleep(5 * propagationTimeout) // make sure the propagationTimeout has passed and the gap handler triggered

        then:
        ex.getStreamId() == msg1.getStreamId()
        ex.getStreamPartition() == msg1.getStreamPartition()
        ex.getFrom() == new MessageRef(msg1.getTimestamp(), msg1.getSequenceNumber() + 1)
        ex.getTo() == msg4.getPreviousMessageRef()
        ex.getPublisherId() == msg1.getPublisherId()
        ex.getMsgChainId() == msg1.getMsgChainId()
    }

    void "does not throw if different publishers"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0, getPublisherId(1))
        StreamMessage msg4 = createMessage(4, 0, 3, 0, getPublisherId(2))

        when:
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg4)

        then:
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected (same timestamp but different sequence numbers)"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg4 = createMessage(1, 4, 1, 3)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                // GapDetectedException is used just to store the values the gap handler is called with
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })

        when:
        sub.handleRealTimeMessage(msg1)
        then:
        ex == null

        when:
        sub.handleRealTimeMessage(msg4)
        Thread.sleep(5 * propagationTimeout) // make sure the propagationTimeout has passed and the gap handler triggered

        then:
        ex.getStreamId() == msg1.getStreamId()
        ex.getStreamPartition() == msg1.getStreamPartition()
        ex.getFrom() == new MessageRef(msg1.getTimestamp(), msg1.getSequenceNumber() + 1)
        ex.getTo() == msg4.getPreviousMessageRef()
        ex.getPublisherId() == msg1.getPublisherId()
        ex.getMsgChainId() == msg1.getMsgChainId()
    }

    void "does not throw if there is no gap"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage msg2 = createMessage(1, 1, 1, 0)
        StreamMessage msg3 = createMessage(4, 0, 1, 1)

        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId) {
                throw new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })

        when:
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg2)
        sub.handleRealTimeMessage(msg3)
        Thread.sleep(5 * propagationTimeout) // make sure the propagationTimeout has passed to allow the gap handler to trigger

        then:
        received.size() == 3
        noExceptionThrown()
    }

    void "decrypts encrypted messages with the correct key"() {
        GroupKey groupKey = GroupKey.generate()

        Map plaintext = [foo: 'bar']
        StreamMessage msg1 = createMessage(plaintext)

        msg1 = EncryptionUtil.encryptStreamMessage(msg1, groupKey)

        when:
        sub.handleRealTimeMessage(msg1)

        then:
        1 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> groupKey
        received[0].getParsedContent() == plaintext
    }

    void "reports new group keys to the keyExchangeUtil"() {
        GroupKey oldKey = GroupKey.generate()
        GroupKey newKey = GroupKey.generate()
        StreamMessage msg = createMessage()
        msg = EncryptionUtil.encryptStreamMessage(msg, oldKey)
        msg = new StreamMessage.Builder(msg)
                .withNewGroupKey(EncryptionUtil.encryptGroupKey(newKey, oldKey))
                .createStreamMessage()

        when:
        sub.handleRealTimeMessage(msg)

        then:
        1 * keyStore.get(msg.getStreamId(), oldKey.getGroupKeyId()) >> oldKey
        1 * keyExchangeUtil.handleNewAESEncryptedKeys([msg.getNewGroupKey()], msg.getStreamId(), msg.getPublisherId(), msg.getGroupKeyId())
    }

    void "calls key request function if the key is not in the key store (multiple times if there's no response)"() {
        GroupKey groupKey = GroupKey.generate()
        msg = EncryptionUtil.encryptStreamMessage(msg, groupKey)

        Address receivedPublisherId = null
        int nbCalls = 0
        int timeout = 3000
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, defaultHandler, keyStore, keyExchangeUtil,
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
        1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in store
        nbCalls == 3

        when:
        sub.onNewKeysAdded(msg.getPublisherId(), [groupKey])
        Thread.sleep(timeout * 2)

        then:
        1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> groupKey // key is now found
        receivedPublisherId == msg.getPublisherId()
        nbCalls == 3
    }

    void "calls key request function MAX_NB_GROUP_KEY_REQUESTS times"() {
        GroupKey groupKey = GroupKey.generate()
        msg = EncryptionUtil.encryptStreamMessage(msg, groupKey)

        int nbCalls = 0
        int timeout = 200
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, defaultHandler, keyStore, keyExchangeUtil,
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
        1 * keyStore.get(msg.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in store
        nbCalls == BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS
    }

    void "queues messages when not able to decrypt and handles them once the key is updated"() {
        StreamMessage msg1 = createMessage(1, [foo: 'bar1'])
        StreamMessage msg2 = createMessage(2, [foo: 'bar2'])

        GroupKey groupKey = GroupKey.generate()
        msg1 = EncryptionUtil.encryptStreamMessage(msg1, groupKey)
        msg2 = EncryptionUtil.encryptStreamMessage(msg2, groupKey)

        when:
        // Cannot decrypt msg1, queues it and calls the key request function
        sub.handleRealTimeMessage(msg1)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        1 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> null // key not found in store
        groupKeyRequestCount == 1

        when:
        // Cannot decrypt msg2, queues it but doesn't call the key request function
        sub.handleRealTimeMessage(msg2)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        0 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) // not called because the request for the key is in progress
        groupKeyRequestCount == 1

        // faking the reception of the group key response
        when:
        sub.onNewKeysAdded(msg1.getPublisherId(), [groupKey])

        then:
        2 * keyStore.get(msg1.getStreamId(), groupKey.getGroupKeyId()) >> groupKey // key now found for both messages
        received.size() == 2
        received[0].getParsedContent() == [foo: 'bar1']
        received[1].getParsedContent() == [foo: 'bar2']
        groupKeyRequestCount == 1
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, getPublisherId(1), [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, getPublisherId(1), [foo: 'bar2'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, getPublisherId(2), [foo: 'bar3'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, getPublisherId(2), [foo: 'bar4'])

        GroupKey groupKeyPub1 = GroupKey.generate()
        GroupKey groupKeyPub2 = GroupKey.generate()

        msg1pub1 = EncryptionUtil.encryptStreamMessage(msg1pub1, groupKeyPub1)
        msg2pub1 = EncryptionUtil.encryptStreamMessage(msg2pub1, groupKeyPub1)

        msg1pub2 = EncryptionUtil.encryptStreamMessage(msg1pub2, groupKeyPub2)
        msg2pub2 = EncryptionUtil.encryptStreamMessage(msg2pub2, groupKeyPub2)

        when:
        // Cannot decrypt msg1pub1, queues it and calls the key request function
        sub.handleRealTimeMessage(msg1pub1)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> null // key not found in store
        groupKeyRequestCount == 1

        when:
        // Cannot decrypt msg2pub1, queues it.
        sub.handleRealTimeMessage(msg2pub1)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        0 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) // not called because the request for the key is in progress
        groupKeyRequestCount == 1

        when:
        // Cannot decrypt msg1pub2, queues it and calls the key request function
        sub.handleRealTimeMessage(msg1pub2)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        1 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> null // key not found in store
        groupKeyRequestCount == 2

        when:
        // Cannot decrypt msg2pub2, queues it.
        sub.handleRealTimeMessage(msg2pub2)

        then:
        groupKeyRequestCount == 2
        0 * keyStore.get(msg2pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) // not called because the request for the key is in progress

        when:
        // faking the reception of the group key response
        sub.onNewKeysAdded(msg1pub1.getPublisherId(), [groupKeyPub1])
        sub.onNewKeysAdded(msg1pub2.getPublisherId(), [groupKeyPub2])

        then:
        2 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
        2 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> groupKeyPub2
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']
        received.get(2).getParsedContent() == [foo: 'bar3']
        received.get(3).getParsedContent() == [foo: 'bar4']
        groupKeyRequestCount == 2
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers interleaved)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, getPublisherId(1), [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, getPublisherId(1), [foo: 'bar2'])
        StreamMessage msg3pub1 = createMessage(3, 0, null, null, getPublisherId(1), [foo: 'bar3'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, getPublisherId(2), [foo: 'bar4'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, getPublisherId(2), [foo: 'bar5'])

        GroupKey groupKeyPub1 = GroupKey.generate()
        GroupKey groupKeyPub2 = GroupKey.generate()

        [msg1pub1, msg2pub1, msg3pub1].each {it = EncryptionUtil.encryptStreamMessage(it, groupKeyPub1) }
        msg1pub1 = EncryptionUtil.encryptStreamMessage(msg1pub1, groupKeyPub1)
        msg2pub1 = EncryptionUtil.encryptStreamMessage(msg2pub1, groupKeyPub1)
        msg3pub1 = EncryptionUtil.encryptStreamMessage(msg3pub1, groupKeyPub1)

        msg1pub2 = EncryptionUtil.encryptStreamMessage(msg1pub2, groupKeyPub2)
        msg2pub2 = EncryptionUtil.encryptStreamMessage(msg2pub2, groupKeyPub2)

        when:
        sub.handleRealTimeMessage(msg1pub1)
        sub.handleRealTimeMessage(msg1pub2)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> null // key not found in store
        1 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> null // key not found in store
        groupKeyRequestCount == 2

        when:
        sub.handleRealTimeMessage(msg2pub1) // queued
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        groupKeyRequestCount == 2
        0 * keyStore.get(msg2pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) // not called because the request for the key is in progress

        when:
        // Triggers processing of queued messages for pub1
        sub.onNewKeysAdded(getPublisherId(1), [groupKeyPub1])

        then:
        2 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
        groupKeyRequestCount == 2
        received.size() == 2
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']

        when:
        // Processed immediately because now we have the key
        sub.handleRealTimeMessage(msg3pub1)

        then:
        1 * keyStore.get(msg1pub1.getStreamId(), groupKeyPub1.getGroupKeyId()) >> groupKeyPub1
        received.size() == 3
        received.get(2).getParsedContent() == [foo: 'bar3']

        when:
        sub.handleRealTimeMessage(msg2pub2) // queued, because no key for pub2 yet
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        received.size() == 3
        groupKeyRequestCount == 2

        when:
        // Triggers processing of queued messages for pub2
        sub.onNewKeysAdded(getPublisherId(2), [groupKeyPub2])

        then:
        2 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> groupKeyPub2
        received.size() == 5
        received.get(3).getParsedContent() == [foo: 'bar4']
        received.get(4).getParsedContent() == [foo: 'bar5']
        groupKeyRequestCount == 2
    }


    void "queues messages when not able to decrypt and handles them once the key is updated (one publisher, two keys on two msgChains)"() {
        // All messages have the same publisherId
        StreamMessage key1msg1 = createMessage(1, 0, null, null, getPublisherId(1), [n: 1], "msgChain1")
        StreamMessage key1msg2 = createMessage(2, 0, null, null, getPublisherId(1), [n: 2], "msgChain1")
        StreamMessage key2msg1 = createMessage(3, 0, null, null, getPublisherId(1), [n: 3], "msgChain2")
        StreamMessage key2msg2 = createMessage(4, 0, null, null, getPublisherId(1), [n: 4], "msgChain2")

        GroupKey key1 = GroupKey.generate()
        GroupKey key2 = GroupKey.generate()

        key1msg1 = EncryptionUtil.encryptStreamMessage(key1msg1, key1)
        key1msg2 = EncryptionUtil.encryptStreamMessage(key1msg2, key1)

        key2msg1 = EncryptionUtil.encryptStreamMessage(key2msg1, key2)
        key2msg2 = EncryptionUtil.encryptStreamMessage(key2msg2, key2)

        when:
        sub.handleRealTimeMessage(key1msg1)
        sub.handleRealTimeMessage(key2msg1)
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        1 * keyStore.get(key1msg1.getStreamId(), key1.getGroupKeyId()) >> null // key not found in store
        1 * keyStore.get(key2msg1.getStreamId(), key2.getGroupKeyId()) >> null // key not found in store
        groupKeyRequestCount == 2

        when:
        sub.handleRealTimeMessage(key1msg2) // queued
        sub.handleRealTimeMessage(key2msg2) // queued
        // group key request function gets called asynchronously, make sure there's time to call it
        Thread.sleep(100)

        then:
        groupKeyRequestCount == 2
        0 * keyStore.get(_, _) // not called because the request for both keys is in progress

        when:
        // Triggers processing of queued messages for key1 / msgChain1
        sub.onNewKeysAdded(getPublisherId(1), [key1])

        then:
        2 * keyStore.get(key1msg1.getStreamId(), key1.getGroupKeyId()) >> key1
        groupKeyRequestCount == 2
        received.size() == 2
        received.get(0).getParsedContent() == [n: 1]
        received.get(1).getParsedContent() == [n: 2]

        when:
        // Triggers processing of queued messages for key2 / msgChain2
        sub.onNewKeysAdded(getPublisherId(1), [key2])

        then:
        2 * keyStore.get(key2msg1.getStreamId(), key2.getGroupKeyId()) >> key2
        received.size() == 4
        received.get(2).getParsedContent() == [n: 3]
        received.get(3).getParsedContent() == [n: 4]
        groupKeyRequestCount == 2
    }

    void "calls onUnableToDecrypt handler when not able to decrypt after receiving key"() {
        GroupKey correctGroupKey = GroupKey.generate()
        GroupKey incorrectGroupKeyWithCorrectId = new GroupKey(correctGroupKey.getGroupKeyId(), GroupKey.generate().getGroupKeyHex())

        msg = EncryptionUtil.encryptStreamMessage(msg, correctGroupKey)

        when:
        sub.handleRealTimeMessage(msg) // queues message
        sub.onNewKeysAdded(msg.getPublisherId(), [incorrectGroupKeyWithCorrectId])

        then:
        unableToDecryptCount == 1
    }

    // TODO: good test?
    void "doesn't throw when some other key is added (?)"() {
        GroupKey correctGroupKey = GroupKey.generate()
        GroupKey otherKey = GroupKey.generate()

        msg = EncryptionUtil.encryptStreamMessage(msg, correctGroupKey)

        when:
        sub.handleRealTimeMessage(msg) // queues message
        sub.onNewKeysAdded(msg.getPublisherId(), [otherKey]) // other key added (not waiting for this key)

        then:
        noExceptionThrown()
        //thrown(UnableToDecryptException)
    }

}
