package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.OrderedMsgChain
import com.streamr.client.utils.GroupKey
import org.apache.commons.codec.binary.Hex
import spock.util.concurrent.PollingConditions

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import java.security.SecureRandom

class RealTimeSubscriptionSpec extends StreamrSpecification {

    StreamMessage msg

    def setup() {
        msg = createMessage()
    }

    MessageHandler empty = new MessageHandler() {
        @Override
        void onMessage(Subscription sub, StreamMessage message) {

        }
    }

    GroupKey genKey() {
        byte[] keyBytes = new byte[32]
        secureRandom.nextBytes(keyBytes)
        return new GroupKey(Hex.encodeHexString(keyBytes))
    }

    SecureRandom secureRandom = new SecureRandom()

    void "calls the message handler"() {
        StreamMessage received

        when:
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
            }
        })
        sub.handleRealTimeMessage(msg)
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
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received.add(message)
            }
        })
        for (int i=0;i<5;i++) {
            sub.handleRealTimeMessage(msgs.get(i))
        }
        then:
        for (int i=0;i<5;i++) {
            assert msgs.get(i).serialize() == received.get(i).serialize()
        }
    }

    void "handles resent messages during resending"() {
        StreamMessage received
        when:
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
            }
        })
        sub.setResending(true)
        sub.handleResentMessage(msg)
        then:
        received.serialize() == msg.serialize()
    }

    void "ignores duplicate messages"() {
        StreamMessage received
        int counter = 0
        when:
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
                counter++
                if (counter == 2) {
                    throw new Exception("Shouldn't handle this duplicate message!")
                }
            }
        })
        sub.handleRealTimeMessage(msg)
        sub.handleRealTimeMessage(msg)
        then:
        received.serialize() == msg.serialize()
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, new HashMap<String, String>(), null, 10L, 10L, false)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg4)
        then:
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected (same timestamp but different sequence numbers)"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(1, 4, 1, 3)
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, new HashMap<String, String>(), null, 10L, 10L, false)
        GapDetectedException ex
        sub.setGapHandler(new OrderedMsgChain.GapHandlerFunction() {
            @Override
            void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId) {
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
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty)
        sub.handleRealTimeMessage(msg1)
        sub.handleRealTimeMessage(msg2)
        sub.handleRealTimeMessage(msg3)
        then:
        noExceptionThrown()
    }

    void "decrypts encrypted messages with the correct key"() {
        GroupKey groupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        Map received = null
        StreamMessage msg1 = createMessage(plaintext)

        EncryptionUtil.encryptStreamMessage(msg1, secretKey)

        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getParsedContent()
            }
        }, ['publisherId': groupKey])
        when:
        sub.handleRealTimeMessage(msg1)
        then:
        received == plaintext
    }

    void "calls key request function when cannot decrypt messages with wrong key (multiple times when no response)"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        EncryptionUtil.encryptStreamMessage(msg, secretKey)

        String receivedPublisherId
        int nbCalls = 0
        int timeout = 3000
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, ['publisherId': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
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
        sub.onNewKeys(msg.getPublisherId(), [groupKey])
        Thread.sleep(timeout * 2)
        then:
        receivedPublisherId == msg.getPublisherId().toLowerCase()
        nbCalls == 3
    }

    void "calls key request function MAX_NB_GROUP_KEY_REQUESTS times"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        EncryptionUtil.encryptStreamMessage(msg, secretKey)

        int nbCalls = 0
        int timeout = 200
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, ['publisherId': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
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

        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        EncryptionUtil.encryptStreamMessage(msg1, secretKey)
        EncryptionUtil.encryptStreamMessage(msg2, secretKey)

        int callCount = 0
        StreamMessage received1 = null
        StreamMessage received2 = null
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                if (received1 == null) {
                    received1 = message
                } else if (received2 == null) {
                    received2 = message
                }
            }
        }, ['publisherId': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                callCount++
            }
        })
        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleRealTimeMessage(msg1)

        then:
        new PollingConditions().within(10) {
            callCount == 1
        }

        when:
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2)

        then:
        callCount == 1

        // faking the reception of the group key response
        when:
        sub.onNewKeys(msg1.getPublisherId(), (ArrayList<GroupKey>)[groupKey])

        then:
        received1.getParsedContent() == [foo: 'bar1']
        received2.getParsedContent() == [foo: 'bar2']
        callCount == 1
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, "publisherId1", [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, "publisherId1", [foo: 'bar2'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, "publisherId2", [foo: 'bar3'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, "publisherId2", [foo: 'bar4'])

        GroupKey groupKey1 = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        GroupKey groupKey2 = genKey()
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")

        EncryptionUtil.encryptStreamMessage(msg1pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg1pub2, secretKey2)
        EncryptionUtil.encryptStreamMessage(msg2pub2, secretKey2)

        int callCount = 0
        ArrayList<StreamMessage> received = []
        RealTimeSubscription sub = new RealTimeSubscription(msg1pub1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received.add(message)
            }
        }, ['publisherId1': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                callCount++
            }
        })
        when:
        // Cannot decrypt msg1, queues it and calls the handler
        sub.handleRealTimeMessage(msg1pub1)
        then:
        new PollingConditions().within(10) {
            callCount == 1
        }

        when:
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2pub1)
        then:
        callCount == 1

        when:
        // Cannot decrypt msg3, queues it and calls the handler
        sub.handleRealTimeMessage(msg1pub2)
        then:
        new PollingConditions().within(10) {
            callCount == 2
        }

        when:
        // Cannot decrypt msg4, queues it.
        sub.handleRealTimeMessage(msg2pub2)
        then:
        callCount == 2

        when:
        // faking the reception of the group key response
        sub.onNewKeys(msg1pub1.getPublisherId(), (ArrayList<GroupKey>)[groupKey1])
        sub.onNewKeys(msg1pub2.getPublisherId(), (ArrayList<GroupKey>)[groupKey2])
        then:
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']
        received.get(2).getParsedContent() == [foo: 'bar3']
        received.get(3).getParsedContent() == [foo: 'bar4']
        callCount == 2
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers interleaved)"() {
        StreamMessage msg1pub1 = createMessage(1, 0, null, null, "publisherId1", [foo: 'bar1'])
        StreamMessage msg2pub1 = createMessage(2, 0, null, null, "publisherId1", [foo: 'bar2'])
        StreamMessage msg3pub1 = createMessage(3, 0, null, null, "publisherId1", [foo: 'bar3'])
        StreamMessage msg1pub2 = createMessage(1, 0, null, null, "publisherId2", [foo: 'bar4'])
        StreamMessage msg2pub2 = createMessage(2, 0, null, null, "publisherId2", [foo: 'bar5'])

        GroupKey groupKey1 = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        GroupKey groupKey2 = genKey()
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")

        EncryptionUtil.encryptStreamMessage(msg1pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg1pub2, secretKey2)
        EncryptionUtil.encryptStreamMessage(msg2pub2, secretKey2)

        int callCount = 0
        ArrayList<StreamMessage> received = []
        RealTimeSubscription sub = new RealTimeSubscription(msg1pub1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received.add(message)
            }
        }, ['publisherId1': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                callCount++
            }
        })
        when:
        sub.handleRealTimeMessage(msg1pub1)
        sub.handleRealTimeMessage(msg1pub2)
        then:
        new PollingConditions().within(10) {
            callCount == 2
        }

        when:
        sub.handleRealTimeMessage(msg2pub1)
        sub.onNewKeys("publisherId1", (ArrayList<GroupKey>)[groupKey1])
        sub.handleRealTimeMessage(msg3pub1)
        sub.handleRealTimeMessage(msg2pub2)
        sub.onNewKeys("publisherId2", (ArrayList<GroupKey>)[groupKey2])

        then:
        received.get(0).getParsedContent() == [foo: 'bar1']
        received.get(1).getParsedContent() == [foo: 'bar2']
        received.get(2).getParsedContent() == [foo: 'bar3']
        received.get(3).getParsedContent() == [foo: 'bar4']
        received.get(4).getParsedContent() == [foo: 'bar5']
        callCount == 2
    }

    void "throws when not able to decrypt for the second time"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        GroupKey otherWrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")

        EncryptionUtil.encryptStreamMessage(msg, secretKey)

        String receivedPublisherId
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
            @Override
            void onUnableToDecrypt(UnableToDecryptException e) {
                throw e
            }
        }, ['publisherId': wrongGroupKey], new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                receivedPublisherId = publisherId
            }
        })
        when:
        sub.handleRealTimeMessage(msg)
        sub.onNewKeys(msg.getPublisherId(), (ArrayList<GroupKey>)[otherWrongGroupKey])
        then:
        thrown(UnableToDecryptException)
    }

    void "decrypts first message, updates key, decrypts second message"() {
        GroupKey groupKey1 = genKey()
        GroupKey groupKey2 = genKey()

        Map content1 = [foo: 'bar']
        StreamMessage msg1 = createMessage(content1)

        EncryptionUtil.encryptStreamMessageAndNewKey(groupKey2.getGroupKeyHex(), msg1, groupKey1.toSecretKey())

        Map content2 = [hello: 'world']
        StreamMessage msg2 = createMessage(content2)

        EncryptionUtil.encryptStreamMessage(msg2, groupKey2.toSecretKey())

        Map received1 = null
        Map received2 = null

        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                if (received1 == null) {
                    received1 = message.getParsedContent()
                } else {
                    received2 = message.getParsedContent()
                }

            }
        }, ['publisherId': groupKey1])
        when:
        sub.handleRealTimeMessage(msg1)
        then:
        received1 == content1
        when:
        sub.handleRealTimeMessage(msg2)
        then:
        received2 == content2
    }
}
