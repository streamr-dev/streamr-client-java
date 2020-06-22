package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyRequestException
import com.streamr.client.exceptions.SigningRequiredException
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import org.apache.commons.codec.binary.Hex
import org.ethereum.crypto.ECKey
import spock.lang.Specification

import java.security.SecureRandom

class MessageCreationUtilSpec extends Specification {
    KeyStorage keyStorage
    MessageCreationUtil msgCreationUtil
    Stream stream
    SecureRandom secureRandom
    Map message

    void setup() {
        keyStorage = new LatestKeyStorage()
        msgCreationUtil = new MessageCreationUtil("publisherId", null, keyStorage)
        stream = new Stream("test-stream", "")
        stream.id = "stream-id"
        stream.partitions = 1
        secureRandom = new SecureRandom()
        message = [foo: "bar"]
    }

    UnencryptedGroupKey genUnencryptedKey(int keyLength) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes), new Date())
    }

    EncryptedGroupKey genEncryptedKey(int keyLength, Date start) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new EncryptedGroupKey(Hex.encodeHexString(keyBytes), start)
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
        MessageCreationUtil msgCreationUtil2 = new MessageCreationUtil("publisherId", signingUtil, keyStorage)
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
        // Messages should go to different partitions
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-1")
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-2")

        then:
        assert msg1.getStreamPartition() != msg2.getStreamPartition()

        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 0L
        assert msg2.previousMessageRef == null
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
        UnencryptedGroupKey key = genUnencryptedKey(32)
        keyStorage.addKey(stream.id, key)
        MessageCreationUtil util = new MessageCreationUtil("publisherId", null, keyStorage)
        when:
        StreamMessageV31 msg = (StreamMessageV31) util.createStreamMessage(stream, message, new Date(), null)
        then:
        assert msg.encryptionType == StreamMessage.EncryptionType.AES
        assert msg.getSerializedContent().length() == 58 // 16*2 + 13*2 (hex string made of IV + msg of 13 chars)
    }

    void "creates encrypted messages when key defined in createStreamMessage() and use the same key later"() {
        UnencryptedGroupKey key = genUnencryptedKey(32)
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key)
        then:
        assert keyStorage.getLatestKey(stream.id) == key
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
        UnencryptedGroupKey key1 = genUnencryptedKey(32)
        UnencryptedGroupKey key2 = genUnencryptedKey(32)
        when:
        StreamMessageV31 msg1 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key1)
        then:
        keyStorage.getLatestKey(stream.id) == key1
        assert msg1.encryptionType == StreamMessage.EncryptionType.AES
        assert msg1.getSerializedContent().length() == 58
        when:
        StreamMessageV31 msg2 = (StreamMessageV31) msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key2)
        then:
        keyStorage.getLatestKey(stream.id) == key2
        assert msg2.encryptionType == StreamMessage.EncryptionType.NEW_KEY_AND_AES
        assert msg2.getSerializedContent().length() == 122 // 16*2 + 32*2 + 13*2 (IV + key of 32 bytes + msg of 13 chars)
    }

    void "should not be able to create unsigned group key request"() {
        when:
        msgCreationUtil.createGroupKeyRequest("", "", "", null, null)
        then:
        SigningRequiredException e = thrown SigningRequiredException
        e.message == "Cannot create unsigned group key request. Must authenticate with an Ethereum account"
    }

    void "creates correct group key request"() {
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        ECKey account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        SigningUtil signingUtil = new SigningUtil(account)
        MessageCreationUtil util = new MessageCreationUtil("subscriberId", signingUtil, keyStorage)
        when:
        StreamMessage msg = util.createGroupKeyRequest(
                "publisherInboxAddress", "streamId", "rsaPublicKey",
                new Date(123), new Date(456))
        then:
        msg.getStreamId() == "publisherInboxAddress".toLowerCase()
        msg.getPublisherId() == "subscriberId"
        msg.getContentType() == StreamMessage.ContentType.GROUP_KEY_REQUEST
        msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
        msg.getContent().get("streamId") == "streamId"
        msg.getContent().get("publicKey") == "rsaPublicKey"
        ((Map<String, Object>) msg.getContent().get("range")).get("start") == 123
        ((Map<String, Object>) msg.getContent().get("range")).get("end") == 456
        msg.getSignature() != null
    }

    void "should not be able to create unsigned group key response"() {
        when:
        msgCreationUtil.createGroupKeyResponse("", "", null)
        then:
        SigningRequiredException e = thrown SigningRequiredException
        e.message == "Cannot create unsigned group key response. Must authenticate with an Ethereum account"
    }

    void "creates correct group key response"() {
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        ECKey account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        SigningUtil signingUtil = new SigningUtil(account)
        MessageCreationUtil util = new MessageCreationUtil("publisherId", signingUtil, keyStorage)
        EncryptedGroupKey k1 = genEncryptedKey(32, new Date(123))
        EncryptedGroupKey k2 = genEncryptedKey(32, new Date(4556))
        when:
        StreamMessage msg = util.createGroupKeyResponse("subscriberInboxAddress", "streamId", [k1, k2])
        then:
        msg.getStreamId() == "subscriberInboxAddress".toLowerCase()
        msg.getContentType() == StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE
        msg.getEncryptionType() == StreamMessage.EncryptionType.RSA
        msg.getContent().get("streamId") == "streamId"
        msg.getContent().get("keys") == [k1.toMap(), k2.toMap()]
        msg.getSignature() != null
    }

    void "should not be able to create unsigned group key reset"() {
        when:
        msgCreationUtil.createGroupKeyReset("", "", null)
        then:
        SigningRequiredException e = thrown SigningRequiredException
        e.message == "Cannot create unsigned group key reset. Must authenticate with an Ethereum account"
    }

    void "creates correct group key reset"() {
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        ECKey account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        SigningUtil signingUtil = new SigningUtil(account)
        MessageCreationUtil util = new MessageCreationUtil("publisherId", signingUtil, keyStorage)
        EncryptedGroupKey k = genEncryptedKey(32, new Date(123))
        when:
        StreamMessage msg = util.createGroupKeyReset("subscriberInboxAddress", "streamId", k)
        then:
        msg.getStreamId() == "subscriberInboxAddress".toLowerCase()
        msg.getContentType() == StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE
        msg.getEncryptionType() == StreamMessage.EncryptionType.RSA
        msg.getContent().get("streamId") == "streamId"
        msg.getContent().get("groupKey") == k.groupKeyHex
        msg.getContent().get("start") == k.getStartTime()
        msg.getSignature() != null
    }

    void "should not be able to create unsigned error message"() {
        when:
        msgCreationUtil.createGroupKeyErrorResponse("", "streamId", "requestId", new Exception())
        then:
        SigningRequiredException e = thrown SigningRequiredException
        e.message == "Cannot create unsigned error message. Must authenticate with an Ethereum account"
    }

    void "creates correct error message"() {
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        ECKey account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        SigningUtil signingUtil = new SigningUtil(account)
        MessageCreationUtil util = new MessageCreationUtil("publisherId", signingUtil, keyStorage)
        when:
        StreamMessage msg = util.createGroupKeyErrorResponse("destinationAddress", "streamId", "requestId", new InvalidGroupKeyRequestException("some error message"))
        then:
        msg.getStreamId() == "destinationAddress".toLowerCase()
        msg.getContentType() == StreamMessage.ContentType.GROUP_KEY_RESPONSE_ERROR
        msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
        msg.getContent().get("requestId") == "requestId"
        msg.getContent().get("streamId") == "streamId"
        msg.getContent().get("code") == "INVALID_GROUP_KEY_REQUEST"
        msg.getContent().get("message") == "some error message"
        msg.getSignature() != null
    }
}
