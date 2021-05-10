package com.streamr.client.subs

import com.streamr.client.MessageHandler
import com.streamr.client.utils.UnableToDecryptException
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription.GroupKeyRequestFunction
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.client.testing.TestingMessageRef
import com.streamr.client.utils.Address
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.GroupKeyStore
import com.streamr.client.utils.KeyExchangeUtil
import com.streamr.client.utils.OrderedMsgChain
import spock.lang.Specification

/**
 * BasicSubscription is abstract, but contains most of the code for RealtimeSubscription and
 * HistoricalSubscription. This class tests BasicSubscription via RealtimeSubscription.
 */
class BasicSubscriptionSpec extends Specification {

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
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(0)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        msg = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
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
        for (long i = 0; i < 5; i++) {
            final MessageId messageId = new MessageId.Builder()
                    .withStreamId("streamId")
                    .withTimestamp(i)
                    .withSequenceNumber(0)
                    .withPublisherId(TestingAddresses.PUBLISHER_ID)
                    .withMsgChainId("msgChainId")
                    .createMessageId()
            msgs.add(new StreamMessage.Builder()
                    .withMessageId(messageId)
                    .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0))
                    .withContent(TestingContent.emptyMessage())
                    .createStreamMessage())
        }

        when:
        for (int i = 0; i < 5; i++) {
            sub.handleRealTimeMessage(msgs.get(i))
        }

        then:
        for (int i = 0; i < 5; i++) {
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
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0))
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
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(4)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(2))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg4 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(3, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()

        when:
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg4)

        then:
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected (same timestamp but different sequence numbers)"() {
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(4)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg4 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(1, 3))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
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
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(1)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(1, 0))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(4)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg3 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(1, 1))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()

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
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(System.currentTimeMillis())
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        Map<String, Object> plaintext = [foo: 'bar']
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withContent(TestingContent.fromJsonMap(plaintext))
                .createStreamMessage()
        GroupKey groupKey = GroupKey.generate()
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
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(0)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.emptyMessage())
                .createStreamMessage()
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
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withContent(TestingContent.fromJsonMap([foo: 'bar1']))
                .createStreamMessage()
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withContent(TestingContent.fromJsonMap([foo: 'bar2']))
                .createStreamMessage()

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
        final MessageId messageId3 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1pub1 = new StreamMessage.Builder()
                .withMessageId(messageId3)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar1']))
                .createStreamMessage()
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2pub1 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar2']))
                .createStreamMessage()
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(2))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1pub2 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar3']))
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(2))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2pub2 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar4']))
                .createStreamMessage()

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
        final MessageId messageId4 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1pub1 = new StreamMessage.Builder()
                .withMessageId(messageId4)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar1']))
                .createStreamMessage()
        final MessageId messageId3 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2pub1 = new StreamMessage.Builder()
                .withMessageId(messageId3)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar2']))
                .createStreamMessage()
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(3)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg3pub1 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar3']))
                .createStreamMessage()
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(2))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg1pub2 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar4']))
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(2))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg2pub2 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([foo: 'bar5']))
                .createStreamMessage()

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
        sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), [groupKeyPub1])

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
        sub.onNewKeysAdded(TestingAddresses.createPublisherId(2), [groupKeyPub2])

        then:
        2 * keyStore.get(msg1pub2.getStreamId(), groupKeyPub2.getGroupKeyId()) >> groupKeyPub2
        received.size() == 5
        received.get(3).getParsedContent() == [foo: 'bar4']
        received.get(4).getParsedContent() == [foo: 'bar5']
        groupKeyRequestCount == 2
    }


    void "queues messages when not able to decrypt and handles them once the key is updated (one publisher, two keys on two msgChains)"() {
        // All messages have the same publisherId
        final MessageId messageId3 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(1)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChain1")
                .createMessageId()
        StreamMessage key1msg1 = new StreamMessage.Builder()
                .withMessageId(messageId3)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([n: 1]))
                .createStreamMessage()
        final MessageId messageId2 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(2)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChain1")
                .createMessageId()
        StreamMessage key1msg2 = new StreamMessage.Builder()
                .withMessageId(messageId2)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([n: 2]))
                .createStreamMessage()
        final MessageId messageId1 = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(3)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChain2")
                .createMessageId()
        StreamMessage key2msg1 = new StreamMessage.Builder()
                .withMessageId(messageId1)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([n: 3]))
                .createStreamMessage()
        final MessageId messageId = new MessageId.Builder()
                .withStreamId("streamId")
                .withTimestamp(4)
                .withSequenceNumber(0)
                .withPublisherId(TestingAddresses.createPublisherId(1))
                .withMsgChainId("msgChain2")
                .createMessageId()
        StreamMessage key2msg2 = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(TestingMessageRef.createMessageRef(null, null))
                .withContent(TestingContent.fromJsonMap([n: 4]))
                .createStreamMessage()

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
        sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), [key1])

        then:
        2 * keyStore.get(key1msg1.getStreamId(), key1.getGroupKeyId()) >> key1
        groupKeyRequestCount == 2
        received.size() == 2
        received.get(0).getParsedContent() == [n: 1]
        received.get(1).getParsedContent() == [n: 2]

        when:
        // Triggers processing of queued messages for key2 / msgChain2
        sub.onNewKeysAdded(TestingAddresses.createPublisherId(1), [key2])

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
