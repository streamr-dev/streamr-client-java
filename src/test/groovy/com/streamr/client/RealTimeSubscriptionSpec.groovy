package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.HttpUtils
import com.streamr.client.utils.OrderedMsgChain
import com.streamr.client.utils.UnencryptedGroupKey
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class RealTimeSubscriptionSpec extends Specification {
    StreamMessageV31 msg = new StreamMessageV31("stream-id", 0, (new Date()).getTime(), 0, "publisherId", "msgChainId",
            null, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

    StreamMessage createMessage(long timestamp, long sequenceNumber, Long previousTimestamp, Long previousSequenceNumber) {
        return createMessage(timestamp, sequenceNumber, previousTimestamp, previousSequenceNumber, "publisherId")
    }

    StreamMessage createMessage(long timestamp, long sequenceNumber, Long previousTimestamp, Long previousSequenceNumber, String publisherId) {
        return new StreamMessageV31("stream-id", 0, timestamp, sequenceNumber, publisherId, "msgChainId",
                previousTimestamp, previousSequenceNumber, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, "{}", StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
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
        RealTimeSubscription sub = new RealTimeSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
            }
        })
        sub.handleRealTimeMessage(msg)
        then:
        received.toJson() == msg.toJson()
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
            assert msgs.get(i).toJson() == received.get(i).toJson()
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
        received.toJson() == msg.toJson()
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
        received.toJson() == msg.toJson()
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
        UnencryptedGroupKey groupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        Map received = null
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, plaintext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        EncryptionUtil.encryptStreamMessage(msg1, secretKey)

        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getContent()
            }
        }, ['publisherId': groupKey])
        when:
        sub.handleRealTimeMessage(msg1)
        then:
        received == plaintext
    }

    void "calls key request function when cannot decrypt messages with wrong key (multiple times when no response)"() {
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        UnencryptedGroupKey groupKey = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        EncryptionUtil.encryptStreamMessage(msg1, secretKey)

        String receivedPublisherId
        int nbCalls = 0
        int timeout = 1000
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
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
        sub.handleRealTimeMessage(msg1)
        Thread.sleep(timeout * 2 + 500)
        sub.setGroupKeys(msg1.getPublisherId(), [groupKey])
        Thread.sleep(timeout * 2)
        then:
        receivedPublisherId == msg1.getPublisherId().toLowerCase()
        nbCalls == 3

    }

    void "calls key request function MAX_NB_GROUP_KEY_REQUESTS times"() {
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        UnencryptedGroupKey groupKey = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        EncryptionUtil.encryptStreamMessage(msg1, secretKey)

        int nbCalls = 0
        int timeout = 200
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
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
        sub.handleRealTimeMessage(msg1)
        Thread.sleep(timeout * (BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS + 2))
        then:
        nbCalls == BasicSubscription.MAX_NB_GROUP_KEY_REQUESTS

    }

    void "queues messages when not able to decrypt and handles them once the key is updated"() {
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar1'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 2, 0, "publisherId", "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar2'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        UnencryptedGroupKey groupKey = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
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
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2)
        // faking the reception of the group key response
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<UnencryptedGroupKey>)[groupKey])
        then:
        callCount == 1
        received1.getContent() == [foo: 'bar1']
        received2.getContent() == [foo: 'bar2']
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers)"() {
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId1", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar1'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 2, 0, "publisherId1", "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar2'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg3 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId2", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar3'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg4 = new StreamMessageV31("streamId", 0, 2, 0, "publisherId2", "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar4'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        UnencryptedGroupKey groupKey1 = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        UnencryptedGroupKey groupKey2 = genKey()
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")

        EncryptionUtil.encryptStreamMessage(msg1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg3, secretKey2)
        EncryptionUtil.encryptStreamMessage(msg4, secretKey2)

        int callCount = 0
        ArrayList<StreamMessage> received = []
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
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
        sub.handleRealTimeMessage(msg1)
        // Cannot decrypt msg2, queues it.
        sub.handleRealTimeMessage(msg2)
        // Cannot decrypt msg3, queues it and calls the handler
        sub.handleRealTimeMessage(msg3)
        // Cannot decrypt msg4, queues it.
        sub.handleRealTimeMessage(msg4)
        // faking the reception of the group key response
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<UnencryptedGroupKey>)[groupKey1])
        sub.setGroupKeys(msg3.getPublisherId(), (ArrayList<UnencryptedGroupKey>)[groupKey2])
        then:
        callCount == 2
        received.get(0).getContent() == [foo: 'bar1']
        received.get(1).getContent() == [foo: 'bar2']
        received.get(2).getContent() == [foo: 'bar3']
        received.get(3).getContent() == [foo: 'bar4']
    }

    void "queues messages when not able to decrypt and handles them once the key is updated (multiple publishers interleaved)"() {
        String publisher1 = "publisherId1"
        String publisher2 = "publisherId2"
        StreamMessageV31 msg1Pub1 = new StreamMessageV31("streamId", 0, 1, 0, publisher1, "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar1'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2Pub1 = new StreamMessageV31("streamId", 0, 2, 0, publisher1, "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar2'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg3Pub1 = new StreamMessageV31("streamId", 0, 3, 0, publisher1, "",
                2, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar3'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg1Pub2 = new StreamMessageV31("streamId", 0, 1, 0, publisher2, "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar4'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2Pub2 = new StreamMessageV31("streamId", 0, 2, 0, publisher2, "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar5'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        UnencryptedGroupKey groupKey1 = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        UnencryptedGroupKey groupKey2 = genKey()
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")

        EncryptionUtil.encryptStreamMessage(msg1Pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg2Pub1, secretKey1)
        EncryptionUtil.encryptStreamMessage(msg1Pub2, secretKey2)
        EncryptionUtil.encryptStreamMessage(msg2Pub2, secretKey2)

        int callCount = 0
        ArrayList<StreamMessage> received = []
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
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
        sub.handleRealTimeMessage(msg1Pub1)
        sub.handleRealTimeMessage(msg1Pub2)
        sub.handleRealTimeMessage(msg2Pub1)
        sub.setGroupKeys(publisher1, (ArrayList<UnencryptedGroupKey>)[groupKey1])
        sub.handleRealTimeMessage(msg3Pub1)
        sub.handleRealTimeMessage(msg2Pub2)
        sub.setGroupKeys(publisher2, (ArrayList<UnencryptedGroupKey>)[groupKey2])

        then:
        callCount == 2
        received.get(0).getContent() == [foo: 'bar1']
        received.get(1).getContent() == [foo: 'bar2']
        received.get(2).getContent() == [foo: 'bar3']
        received.get(3).getContent() == [foo: 'bar4']
        received.get(4).getContent() == [foo: 'bar5']
    }

    void "throws when not able to decrypt for the second time"() {
        UnencryptedGroupKey groupKey = genKey()
        UnencryptedGroupKey wrongGroupKey = genKey()
        UnencryptedGroupKey otherWrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        EncryptionUtil.encryptStreamMessage(msg1, secretKey)

        String receivedPublisherId
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
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
        sub.handleRealTimeMessage(msg1)
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<UnencryptedGroupKey>)[otherWrongGroupKey])
        then:
        thrown(UnableToDecryptException)
    }

    void "decrypts first message, updates key, decrypts second message"() {
        UnencryptedGroupKey groupKey1 = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        byte[] key2Bytes = new byte[32]
        secureRandom.nextBytes(key2Bytes)
        String key2HexString = Hex.encodeHexString(key2Bytes)
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(key2HexString), "AES")

        Map content1 = [foo: 'bar']
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        EncryptionUtil.encryptStreamMessageAndNewKey(key2HexString, msg1, secretKey1)

        Map content2 = [hello: 'world']
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId", "",
                0, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [hello: 'world'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        EncryptionUtil.encryptStreamMessage(msg2, secretKey2)

        Map received1 = null
        Map received2 = null

        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                if (received1 == null) {
                    received1 = message.getContent()
                } else {
                    received2 = message.getContent()
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
