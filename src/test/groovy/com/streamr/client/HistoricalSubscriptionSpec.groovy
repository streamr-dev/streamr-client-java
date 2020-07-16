package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.options.ResendLastOption
import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.HistoricalSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.OrderedMsgChain
import com.streamr.client.utils.UnencryptedGroupKey
import org.apache.commons.codec.binary.Hex
import spock.util.concurrent.PollingConditions

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import java.security.SecureRandom

class HistoricalSubscriptionSpec extends StreamrSpecification {
    StreamMessage msg

    def setup() {
        msg = createMessage([foo: 'bar'])
    }

    MessageHandler empty = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {

        }
    }

    UnencryptedGroupKey genKey() {
        byte[] keyBytes = new byte[32]
        secureRandom.nextBytes(keyBytes)
        return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes))
    }

    SecureRandom secureRandom = new SecureRandom()

    void "calls the message handler"() {
        StreamMessage received
        when:
        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
            }
        }, null)
        sub.handleResentMessage(msg)
        then:
        received.serialize() == msg.serialize()
    }

    void "calls the handler once for each message in order"() {
        ArrayList<StreamMessage> msgs = new ArrayList<>()
        for (int i=0;i<5;i++) {
            msgs.add(createMessage((long)i, 0, null, 0))
        }
        ArrayList<StreamMessage> received = new ArrayList<>()
        when:
        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received.add(message)
            }
        }, null)
        for (int i=0;i<5;i++) {
            sub.handleResentMessage(msgs.get(i))
        }
        then:
        for (int i=0;i<5;i++) {
            assert msgs.get(i).serialize() == received.get(i).serialize()
        }
    }

    void "does not handle real-time messages (queued)"() {
        when:
        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                throw new Exception("Shouldn't handle this message!")
            }
        }, new ResendLastOption(10))
        sub.setResending(true)
        sub.handleRealTimeMessage(msg)
        then:
        noExceptionThrown()
    }

    void "ignores duplicate messages"() {
        StreamMessage received
        int counter = 0
        when:
        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
                counter++
                if (counter == 2) {
                    throw new Exception("Shouldn't handle this duplicate message!")
                }
            }
        }, null)
        sub.handleResentMessage(msg)
        sub.handleResentMessage(msg)
        then:
        received.serialize() == msg.serialize()
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null, new HashMap<String, String>(), null, 10L, 10L, false)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })
        when:
        sub.handleResentMessage(msg1)
        then:
        ex == null

        when:
        sub.handleResentMessage(msg4)
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
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null)
        sub.handleResentMessage(msg1)
        sub.handleResentMessage(msg4)
        then:
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected (same timestamp but different sequence numbers)"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(1, 4, 1, 3)
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null, new HashMap<String, String>(), null, 10L, 10L, false)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
                ex = new GapDetectedException(sub.getStreamId(), sub.getPartition(), from, to, publisherId, msgChainId)
            }
        })
        when:
        sub.handleResentMessage(msg1)
        then:
        ex == null

        when:
        sub.handleResentMessage(msg4)
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
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null)
        sub.handleResentMessage(msg1)
        sub.handleResentMessage(msg2)
        sub.handleResentMessage(msg3)
        then:
        noExceptionThrown()
    }

    void "decrypts encrypted messages with the correct key"() {
        UnencryptedGroupKey key = genKey()
        Map plaintext = msg.getParsedContent()
        EncryptionUtil.encryptStreamMessage(msg, key.getSecretKey())
        Map received = null

        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getParsedContent()
            }
        }, null, ['publisherId': key])

        when:
        sub.handleResentMessage(msg)
        then:
        received == plaintext
    }

    void "calls key request function when no historical group keys are set (multiple times if no response)"() {
        UnencryptedGroupKey key = genKey()
        EncryptionUtil.encryptStreamMessage(msg, key.getSecretKey())
        Map received = null
        String receivedPublisherId = null
        Date receivedStart = null
        Date receivedEnd = null
        int nbCalls = 0
        int timeout = 3000

        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getParsedContent()
            }
        }, new ResendLastOption(1), null, new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                receivedPublisherId = publisherId
                receivedStart = start
                receivedEnd = end
                nbCalls++
            }
        }, timeout, 5000, false)

        when:
        // First call to groupKeyRequestFunction
        sub.handleResentMessage(msg)
        // Wait for 2 timeouts to happen
        Thread.sleep(timeout * 2 + 1500)
        then:
        nbCalls == 3


        when:
        // the group keys are set, no further calls should occur
        sub.setGroupKeys(msg.getPublisherId(), [key])
        Thread.sleep(timeout * 2)
        then:
        receivedPublisherId == msg.getPublisherId().toLowerCase()
        receivedStart == msg.getTimestampAsDate()
        receivedEnd.after(receivedStart)
        nbCalls == 3
    }

    void "queues messages when not able to decrypt and handles them once the keys are set"() {
        UnencryptedGroupKey groupKey1 = genKey()
        UnencryptedGroupKey groupKey2 = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")
        StreamMessage msg1 = createMessage(1, [foo: 'bar1'])
        StreamMessage msg2 = createMessage(2, [foo: 'bar2'])
        EncryptionUtil.encryptStreamMessage(msg1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2, secretKey2)
        int callCount = 0
        StreamMessage received1 = null
        StreamMessage received2 = null
        boolean subDone = false

        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                if (received1 == null) {
                    received1 = message
                } else if (received2 == null) {
                    received2 = message
                }
            }
            @Override
            void done(Subscription sub) {
                subDone = true
            }
        }, new ResendLastOption(1), null, new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                callCount++
            }
        })

        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleResentMessage(msg1)
        // Cannot decrypt msg2, queues it.
        sub.handleResentMessage(msg2)
        // faking the reception of the group key response
        Thread.sleep(100)
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<GroupKey>)[groupKey1, groupKey2])
        sub.endResend()

        then:
        new PollingConditions().within(20) {
            callCount == 1
        }
        received1.getParsedContent() == [foo: 'bar1']
        received2.getParsedContent() == [foo: 'bar2']
        subDone
    }

    void "queues messages when not able to decrypt and handles them once the keys are set (multiple publishers)"() {
        UnencryptedGroupKey groupKey1 = genKey()
        UnencryptedGroupKey groupKey2 = genKey()
        UnencryptedGroupKey groupKey3 = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")
        SecretKey secretKey3 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey3.groupKeyHex), "AES")
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, "publisherId1", [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, "publisherId1", [foo: 'bar2'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, "publisherId2", [foo: 'bar3'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, "publisherId2", [foo: 'bar4'])
        EncryptionUtil.encryptStreamMessage(msg1pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2pub1, secretKey2)
        EncryptionUtil.encryptStreamMessage(msg1pub2, secretKey3)
        EncryptionUtil.encryptStreamMessage(msg2pub2, secretKey3)

        int callCount = 0
        ArrayList<StreamMessage> received = []
        boolean subDone = false

        HistoricalSubscription sub = new HistoricalSubscription(msg1pub1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received.add(message)
            }
            @Override
            void done(Subscription sub) {
                subDone = true
            }
        }, new ResendLastOption(1), null, new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                callCount++
            }
        })

        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleResentMessage(msg1pub1)
        then:
        new PollingConditions().within(10) {
            callCount == 1
        }

        when:
        // Cannot decrypt msg2, queues it.
        sub.handleResentMessage(msg2pub1)
        then:
        callCount == 1

        when:
        // Cannot decrypt msg3, queues it and calls the handler
        sub.handleResentMessage(msg1pub2)
        then:
        new PollingConditions().within(10) {
            callCount == 2
        }

        when:
        // Cannot decrypt msg4, queues it.
        sub.handleResentMessage(msg2pub2)
        then:
        callCount == 2

        when:
        // faking the reception of the group key response
        sub.setGroupKeys(msg1pub2.getPublisherId(), (ArrayList<UnencryptedGroupKey>) [groupKey3])
        sub.setGroupKeys(msg1pub1.getPublisherId(), (ArrayList<UnencryptedGroupKey>) [groupKey1, groupKey2])
        sub.endResend()
        then:
        received.get(0).getParsedContent() == [foo: 'bar3']
        received.get(1).getParsedContent() == [foo: 'bar4']
        received.get(2).getParsedContent() == [foo: 'bar1']
        received.get(3).getParsedContent() == [foo: 'bar2']
        callCount == 2
        subDone
    }

    void "throws when not able to decrypt with historical keys set"() {
        UnencryptedGroupKey key = genKey()
        UnencryptedGroupKey wrongKey = genKey()
        EncryptionUtil.encryptStreamMessage(msg, key.getSecretKey())
        Map received = null
        boolean subDone = false

        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getParsedContent()
            }
            @Override
            void done(Subscription sub) {
                subDone = true
            }
            @Override
            void onUnableToDecrypt(UnableToDecryptException e) {
                throw e
            }
        }, new ResendLastOption(1), ["publisherId": wrongKey], null)
        when:
        sub.handleResentMessage(msg)
        then:
        thrown(UnableToDecryptException)
        !subDone
    }
}
