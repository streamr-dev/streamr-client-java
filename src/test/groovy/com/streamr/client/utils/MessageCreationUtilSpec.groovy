package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyException
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class MessageCreationUtilSpec extends Specification {
    MessageCreationUtil msgCreationUtil
    Stream stream
    SecureRandom secureRandom
    Map message

    void setup() {
        msgCreationUtil = new MessageCreationUtil("publisherId", null)
        stream = new Stream("test-stream", "")
        stream.id = "stream-id"
        stream.partitions = 1
        secureRandom = new SecureRandom()
        message = [foo: "bar"]
    }

    String genKey(int keyLength) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return Hex.encodeHexString(keyBytes)
    }

    void "fields are set. No encryption if no key is defined."() {
        Date timestamp = new Date()
        when:
        StreamMessageV31 msg = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, null)
        then:
        msg.getStreamId() == "stream-id"
        msg.getStreamPartition() == 0
        msg.getTimestamp() == timestamp.getTime()
        msg.getSequenceNumber() == 0L
        msg.getPublisherId() == "publisherId"
        msg.getMsgChainId().length() == 20
        msg.previousMessageRef == null
        msg.contentType == StreamMessage.ContentType.CONTENT_TYPE_JSON
        msg.encryptionType == StreamMessage.EncryptionType.NONE
        msg.content == message
        msg.signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_NONE
        msg.signature == null
    }

    void "signer is called"() {
        SigningUtil signingUtil = Mock(SigningUtil)
        MessageCreationUtil msgCreationUtil2 = new MessageCreationUtil("publisherId", signingUtil)
        when:
        msgCreationUtil2.createStreamMessage(stream, message, new Date(), null)
        then:
        1 * signingUtil.signStreamMessage(_)
    }

    void "publish with sequence numbers equal to 0"() {
        long timestamp = (new Date()).getTime()
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp), null)
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp + 100), null)
        StreamMessageV31 msg3 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp + 200), null)
        then:
        msg1.getTimestamp() == timestamp
        msg1.getSequenceNumber() == 0L
        msg1.previousMessageRef == null

        msg2.getTimestamp() == timestamp + 100
        msg2.getSequenceNumber() == 0L
        msg2.previousMessageRef.timestamp == timestamp
        msg2.previousMessageRef.sequenceNumber == 0L

        msg3.getTimestamp() == timestamp + 200
        msg3.getSequenceNumber() == 0L
        msg3.previousMessageRef.timestamp == timestamp + 100
        msg3.previousMessageRef.sequenceNumber == 0L
    }

    void "publish with increasing sequence numbers"() {
        Date timestamp = new Date()
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, null)
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, null)
        StreamMessageV31 msg3 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, null)
        then:
        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 1L
        assert msg2.previousMessageRef.timestamp == timestamp.getTime()
        assert msg2.previousMessageRef.sequenceNumber == 0L

        assert msg3.getTimestamp() == timestamp.getTime()
        assert msg3.getSequenceNumber() == 2L
        assert msg3.previousMessageRef.timestamp == timestamp.getTime()
        assert msg3.previousMessageRef.sequenceNumber == 1L
    }

    void "publish with sequence numbers equal to 0 (same timestamp but different partitions)"() {
        Date timestamp = new Date()
        stream.partitions = 10
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, null)
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-1")
        StreamMessageV31 msg3 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-2")
        then:
        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 0L
        assert msg2.previousMessageRef == null

        assert msg2.getStreamPartition() != msg3.getStreamPartition()

        assert msg3.getTimestamp() == timestamp.getTime()
        assert msg3.getSequenceNumber() == 0L
        assert msg3.previousMessageRef == null
    }

    void "partitioner behaves correctly"() {
        when:
        stream.partitions = 10
        int[] partitions = [6, 7, 4, 4, 9, 1, 8, 0, 6, 6, 7, 6, 7, 3, 2, 2, 0, 9, 4, 9, 9, 5, 5, 1, 7, 3,
                            0, 6, 5, 6, 3, 6, 3, 5, 6, 2, 3, 6, 7, 2, 1, 3, 2, 7, 1, 1, 5, 1, 4, 0, 1, 9,
                            7, 4, 2, 3, 2, 9, 7, 7, 4, 3, 5, 4, 5, 3, 9, 0, 4, 8, 1, 7, 4, 8, 1, 2, 9, 9,
                            5, 3, 5, 0, 9, 4, 3, 9, 6, 7, 8, 6, 4, 6, 0, 1, 1, 5, 8, 3, 9, 7]
        then:
        for (int i = 0; i < 100; i++) {
            StreamMessageV31 msg = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), "key-"+i)
            assert msg.streamPartition == partitions[i]
        }
    }

    void "creates encrypted messages when key defined in constructor"() {
        String key = genKey(32)
        MessageCreationUtil util = new MessageCreationUtil("publisherId", null, [(stream.id): key])
        when:
        StreamMessageV31 msg = (StreamMessageV31) util.createStreamMessage(stream, message, new Date(), null)
        then:
        assert msg.encryptionType == StreamMessage.EncryptionType.AES
        assert msg.getSerializedContent().length() == 58 // 16*2 + 13*2 (hex string made of IV + msg of 13 chars)
    }

    void "throws if the key is not 256 bits long"() {
        String key = genKey(16)
        when:
        new MessageCreationUtil("publisherId", null, [(stream.id): key])
        then:
        thrown InvalidGroupKeyException

        when:
        msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key)
        then:
        thrown InvalidGroupKeyException
    }

    void "creates encrypted messages when key defined in createStreamMessage() and use the same key later"() {
        String key = genKey(32)
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key)
        then:
        assert msg1.encryptionType == StreamMessage.EncryptionType.AES
        assert msg1.getSerializedContent().length() == 58
        when:
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null)
        then:
        assert msg2.encryptionType == StreamMessage.EncryptionType.AES
        assert msg2.getSerializedContent().length() == 58
        // should use different IVs
        assert msg1.getSerializedContent().substring(0, 32) != msg2.getSerializedContent().substring(0, 32)
        // should produce different ciphertexts of the same plaintext using the same key
        assert msg1.getSerializedContent().substring(32) != msg2.getSerializedContent().substring(32)
    }

    void "should update the key when redefined"() {
        String key1 = genKey(32)
        String key2 = genKey(32)
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key1)
        then:
        assert msg1.encryptionType == StreamMessage.EncryptionType.AES
        assert msg1.getSerializedContent().length() == 58
        when:
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key2)
        then:
        assert msg2.encryptionType == StreamMessage.EncryptionType.NEW_KEY_AND_AES
        assert msg2.getSerializedContent().length() == 122 // 16*2 + 32*2 + 13*2 (IV + key of 32 bytes + msg of 13 chars)
    }
}
