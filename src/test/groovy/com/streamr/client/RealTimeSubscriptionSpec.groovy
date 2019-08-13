package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.subs.RealTimeSubscription
import com.streamr.client.subs.Subscription
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.HttpUtils
import com.streamr.client.utils.OrderedMsgChain
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
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, new HashMap<String, String>(), null, 10L, 10L)
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
        RealTimeSubscription sub = new RealTimeSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, new HashMap<String, String>(), null, 10L, 10L)
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
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), secretKey)
        Map received = null
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
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

    void "calls the key handler function when not able to decrypt encrypted messages with the wrong key"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), secretKey)
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        String receivedPublisherId
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, ['publisherId': wrongGroupKey], new Subscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                receivedPublisherId = publisherId
            }
        })
        when:
        sub.handleRealTimeMessage(msg1)
        then:
        receivedPublisherId == msg1.getPublisherId()
    }

    void "queues messages when not able to decrypt and handles them once the key is updated"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        Map plaintext1 = [foo: 'bar1']
        String ciphertext1 = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext1).getBytes(StandardCharsets.UTF_8), secretKey)
        Map plaintext2 = [foo: 'bar2']
        String ciphertext2 = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext2).getBytes(StandardCharsets.UTF_8), secretKey)
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext1, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 2, 0, "publisherId", "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext2, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
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
        }, ['publisherId': wrongGroupKey], new Subscription.GroupKeyRequestFunction() {
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
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<GroupKey>)[groupKey])
        then:
        callCount == 1
        received1.toJson() == msg1.toJson()
        received2.toJson() == msg2.toJson()
    }

    void "throws when not able to decrypt for the second time"() {
        GroupKey groupKey = genKey()
        GroupKey wrongGroupKey = genKey()
        GroupKey otherWrongGroupKey = genKey()
        SecretKey secretKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), secretKey)
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        String receivedPublisherId
        RealTimeSubscription sub = new RealTimeSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
            }
        }, ['publisherId': wrongGroupKey], new Subscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                receivedPublisherId = publisherId
            }
        })
        when:
        sub.handleRealTimeMessage(msg1)
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<GroupKey>)[otherWrongGroupKey])
        then:
        thrown(UnableToDecryptException)
    }

    void "decrypts first message, updates key, decrypts second message"() {
        GroupKey groupKey1 = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        byte[] key2Bytes = new byte[32]
        secureRandom.nextBytes(key2Bytes)
        GroupKey groupKey2 = new GroupKey(Hex.encodeHexString(key2Bytes))
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")

        Map content1 = [foo: 'bar']
        byte[] content1Bytes = HttpUtils.mapAdapter.toJson(content1).getBytes(StandardCharsets.UTF_8)
        byte[] plaintext1 = new byte[key2Bytes.length + content1Bytes.length]
        System.arraycopy(key2Bytes, 0, plaintext1, 0, key2Bytes.length)
        System.arraycopy(content1Bytes, 0, plaintext1, key2Bytes.length, content1Bytes.length)
        String ciphertext1 = EncryptionUtil.encrypt(plaintext1, secretKey1)
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NEW_KEY_AND_AES, ciphertext1, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

        Map content2 = [hello: 'world']
        String ciphertext2 = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(content2).getBytes(StandardCharsets.UTF_8), secretKey2)
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId", "",
                0, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext2, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

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
