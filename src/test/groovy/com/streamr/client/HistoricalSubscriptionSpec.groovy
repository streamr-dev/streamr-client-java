package com.streamr.client

import com.streamr.client.exceptions.GapDetectedException
import com.streamr.client.exceptions.UnableToDecryptException
import com.streamr.client.options.ResendLastOption
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.subs.BasicSubscription
import com.streamr.client.subs.HistoricalSubscription
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

class HistoricalSubscriptionSpec extends Specification {
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
        HistoricalSubscription sub = new HistoricalSubscription(msg.getStreamId(), msg.getStreamPartition(), new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message
            }
        }, null)
        sub.handleResentMessage(msg)
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
            assert msgs.get(i).toJson() == received.get(i).toJson()
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
        received.toJson() == msg.toJson()
        noExceptionThrown()
    }

    void "calls the gap handler if a gap is detected"() {
        StreamMessage msg1 = createMessage(1, 0, null, 0)
        StreamMessage afterMsg1 = createMessage(1, 1, null, 0)
        StreamMessage msg4 = createMessage(4, 0, 3, 0)
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null, new HashMap<String, String>(), null, 10L, 10L)
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
        HistoricalSubscription sub = new HistoricalSubscription(msg1.getStreamId(), msg1.getStreamPartition(), empty, null, new HashMap<String, String>(), null, 10L, 10L)
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
        SecretKey groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(key.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), groupKey)
        Map received = null
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        HistoricalSubscription sub = new HistoricalSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getContent()
            }
        }, null, ['publisherId': key])
        when:
        sub.handleResentMessage(msg1)
        then:
        received == plaintext
    }

    void "calls the key handler function when no historical group keys are set"() {
        UnencryptedGroupKey key = genKey()
        SecretKey groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(key.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), groupKey)
        Map received = null
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        String receivedPublisherId = null
        Date receivedStart = null
        Date receivedEnd = null
        HistoricalSubscription sub = new HistoricalSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getContent()
            }
        }, new ResendLastOption(1), null, new BasicSubscription.GroupKeyRequestFunction() {
            @Override
            void apply(String publisherId, Date start, Date end) {
                receivedPublisherId = publisherId
                receivedStart = start
                receivedEnd = end
            }
        })
        when:
        sub.handleResentMessage(msg1)
        then:
        receivedPublisherId == msg1.getPublisherId()
        receivedStart == msg1.getTimestampAsDate()
        receivedEnd.after(receivedStart)
    }

    void "queues messages when not able to decrypt and handles them once the keys are set"() {
        UnencryptedGroupKey groupKey1 = genKey()
        UnencryptedGroupKey groupKey2 = genKey()
        SecretKey secretKey1 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey1.groupKeyHex), "AES")
        SecretKey secretKey2 = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKey2.groupKeyHex), "AES")
        Map plaintext1 = [foo: 'bar1']
        String ciphertext1 = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext1).getBytes(StandardCharsets.UTF_8), secretKey1)
        Map plaintext2 = [foo: 'bar2']
        String ciphertext2 = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext2).getBytes(StandardCharsets.UTF_8), secretKey2)
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 1, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext1, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        StreamMessageV31 msg2 = new StreamMessageV31("streamId", 0, 2, 0, "publisherId", "",
                1, 0, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext2, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        int callCount = 0
        StreamMessage received1 = null
        StreamMessage received2 = null
        HistoricalSubscription sub = new HistoricalSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                if (received1 == null) {
                    received1 = message
                } else if (received2 == null) {
                    received2 = message
                }
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
        sub.setGroupKeys(msg1.getPublisherId(), (ArrayList<GroupKey>)[groupKey1, groupKey2])
        then:
        callCount == 1
        received1.toJson() == msg1.toJson()
        received2.toJson() == msg2.toJson()
    }

    void "throws when not able to decrypt with historical keys set"() {
        UnencryptedGroupKey key = genKey()
        UnencryptedGroupKey wrongKey = genKey()
        SecretKey groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(key.groupKeyHex), "AES")
        Map plaintext = [foo: 'bar']
        String ciphertext = EncryptionUtil.encrypt(HttpUtils.mapAdapter.toJson(plaintext).getBytes(StandardCharsets.UTF_8), groupKey)
        Map received = null
        StreamMessageV31 msg1 = new StreamMessageV31("streamId", 0, 0, 0, "publisherId", "",
                null, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.AES, ciphertext, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        HistoricalSubscription sub = new HistoricalSubscription("streamId", 0, new MessageHandler() {
            @Override
            void onMessage(Subscription sub, StreamMessage message) {
                received = message.getContent()
            }
        }, new ResendLastOption(1), ["publisherId": wrongKey], null)
        when:
        sub.handleResentMessage(msg1)
        then:
        thrown(UnableToDecryptException)
    }
}
