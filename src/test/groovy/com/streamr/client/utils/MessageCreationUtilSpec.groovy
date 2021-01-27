package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyRequestException
import com.streamr.client.exceptions.SigningRequiredException
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage
import com.streamr.client.protocol.message_layer.GroupKeyAnnounce
import com.streamr.client.protocol.message_layer.GroupKeyErrorResponse
import com.streamr.client.protocol.message_layer.GroupKeyRequest
import com.streamr.client.protocol.message_layer.GroupKeyResponse
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.rest.Stream
import com.streamr.client.testing.TestingAddresses
import java.security.SecureRandom
import org.web3j.crypto.ECKeyPair
import spock.lang.Specification

class MessageCreationUtilSpec extends Specification {
    MessageCreationUtil msgCreationUtil
    SigningUtil signingUtil
    EncryptionUtil encryptionUtil

    Stream stream
    SecureRandom secureRandom
    Map message

    void setup() {
        stream = new Stream.Builder()
                .withName("test-stream")
                .withDescription("")
                .withId("stream-id")
                .withPartitions(1)
                .withRequireSignedData(false)
                .withRequireEncryptedData(false)
                .createStream()
        secureRandom = new SecureRandom()
        message = [foo: "bar"]

        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        ECKeyPair account = ECKeyPair.create(new BigInteger(withoutPrefix, 16));
        signingUtil = new SigningUtil(account)

        encryptionUtil = new EncryptionUtil()
        msgCreationUtil = new MessageCreationUtil(TestingAddresses.PUBLISHER_ID, signingUtil)
    }

    void "createStreamMessage() creates a StreamMessage with correct values"() {
        Date timestamp = new Date()

        when:
        StreamMessage msg = msgCreationUtil.createStreamMessage(stream, message, timestamp)

        then:
        msg.getStreamId() == stream.getId()
        msg.getStreamPartition() == 0
        msg.getTimestamp() == timestamp.getTime()
        msg.getSequenceNumber() == 0L
        msg.getPublisherId() == TestingAddresses.PUBLISHER_ID
        msg.getMsgChainId().length() == 20
        msg.previousMessageRef == null
        msg.messageType == StreamMessage.MessageType.STREAM_MESSAGE
        msg.contentType == StreamMessage.Content.ContentType.JSON
        msg.encryptionType == StreamMessage.EncryptionType.NONE
        msg.parsedContent == message
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature != null
    }

    void "createStreamMessage() doesn't sign messages if SigningUtil is not defined"() {
        MessageCreationUtil msgCreationUtil2 = new MessageCreationUtil(TestingAddresses.PUBLISHER_ID, null)

        when:
        StreamMessage msg = msgCreationUtil2.createStreamMessage(stream, message, new Date())

        then:
        msg.getSignatureType() == StreamMessage.SignatureType.NONE
        msg.getSignature() == null
    }

    void "createStreamMessage() encrypts the messages if a GroupKey is passed"() {
        GroupKey key = GroupKey.generate()
        when:
        StreamMessage msg = msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key, null)
        then:
        msg.getEncryptionType() == StreamMessage.EncryptionType.AES
        msg.getGroupKeyId() == key.getGroupKeyId()
    }

    void "createStreamMessage() encrypts the new key if a current key and new key are passed"() {
        GroupKey key = GroupKey.generate()
        GroupKey newKey = GroupKey.generate()
        when:
        StreamMessage msg = msgCreationUtil.createStreamMessage(stream, message, new Date(), null, key, newKey)
        then:
        msg.getEncryptionType() == StreamMessage.EncryptionType.AES
        msg.getGroupKeyId() == key.getGroupKeyId()
        EncryptionUtil.decryptGroupKey(msg.getNewGroupKey(), key) == newKey
    }

    void "createStreamMessage() with different timestamps chains messages with sequenceNumber always zero"() {
        long timestamp = (new Date()).getTime()
        when:
        StreamMessage msg1 = msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp))
        StreamMessage msg2 = msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp + 100))
        StreamMessage msg3 = msgCreationUtil.createStreamMessage(stream, message, new Date(timestamp + 200))
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

    void "createStreamMessage() with the same timestamp chains messages with increasing sequenceNumbers"() {
        Date timestamp = new Date()
        when:
        StreamMessage msg1 = msgCreationUtil.createStreamMessage(stream, message, timestamp)
        StreamMessage msg2 = msgCreationUtil.createStreamMessage(stream, message, timestamp)
        StreamMessage msg3 = msgCreationUtil.createStreamMessage(stream, message, timestamp)
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

    void "createStreamMessage() with same timestamps on different partitions chains messages with sequenceNumber always zero"() {
        Date timestamp = new Date()
        stream = new Stream.Builder(stream)
                .withPartitions(10)
                .createStream()

        when:
        // Messages should go to different partitions
        StreamMessage msg1 = msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-1")
        StreamMessage msg2 = msgCreationUtil.createStreamMessage(stream, message, timestamp, "partition-key-2")

        then:
        assert msg1.getStreamPartition() != msg2.getStreamPartition()

        assert msg1.getTimestamp() == timestamp.getTime()
        assert msg1.getSequenceNumber() == 0L
        assert msg1.previousMessageRef == null

        assert msg2.getTimestamp() == timestamp.getTime()
        assert msg2.getSequenceNumber() == 0L
        assert msg2.previousMessageRef == null
    }

    void "createStreamMessage() correctly assigns partitions based on the given partitionKey"() {
        when:
        stream = new Stream.Builder(stream)
                .withPartitions(10)
                .createStream()
        int[] partitions = [6, 7, 4, 4, 9, 1, 8, 0, 6, 6, 7, 6, 7, 3, 2, 2, 0, 9, 4, 9, 9, 5, 5, 1, 7, 3,
                            0, 6, 5, 6, 3, 6, 3, 5, 6, 2, 3, 6, 7, 2, 1, 3, 2, 7, 1, 1, 5, 1, 4, 0, 1, 9,
                            7, 4, 2, 3, 2, 9, 7, 7, 4, 3, 5, 4, 5, 3, 9, 0, 4, 8, 1, 7, 4, 8, 1, 2, 9, 9,
                            5, 3, 5, 0, 9, 4, 3, 9, 6, 7, 8, 6, 4, 6, 0, 1, 1, 5, 8, 3, 9, 7]
        then:
        for (int i = 0; i < 100; i++) {
            StreamMessage msg = msgCreationUtil.createStreamMessage(stream, message, new Date(), "key-"+i)
            assert msg.streamPartition == partitions[i]
        }
    }

    void "createGroupKeyRequest() should throw if SigningUtil is not set"() {
        msgCreationUtil = new MessageCreationUtil(TestingAddresses.SUBSCRIBER_ID, null)

        when:
        msgCreationUtil.createGroupKeyRequest(TestingAddresses.PUBLISHER_ID, "streamId", "", ["keyId1"])
        then:
        thrown SigningRequiredException
    }

    void "createGroupKeyRequest() creates correct group key request"() {
        MessageCreationUtil util = new MessageCreationUtil(TestingAddresses.SUBSCRIBER_ID, signingUtil)

        when:
        StreamMessage msg = util.createGroupKeyRequest(
                TestingAddresses.PUBLISHER_ID, "streamId", "rsaPublicKey", ["keyId1"])
        GroupKeyRequest request = (GroupKeyRequest) AbstractGroupKeyMessage.deserialize(msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_REQUEST)

        then:
        msg.getStreamId() == KeyExchangeUtil.getKeyExchangeStreamId(TestingAddresses.PUBLISHER_ID)
        msg.getPublisherId() == TestingAddresses.SUBSCRIBER_ID
        msg.getMessageType() == StreamMessage.MessageType.GROUP_KEY_REQUEST
        msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
        msg.getSignature() != null
        request.getStreamId() == "streamId"
        request.getPublicKey() == "rsaPublicKey"
        request.getGroupKeyIds() == ["keyId1"]
    }

    void "createGroupKeyResponse() should throw if SigningUtil is not set"() {
        msgCreationUtil = new MessageCreationUtil(TestingAddresses.PUBLISHER_ID, null)
        GroupKey key = GroupKey.generate()
        GroupKeyRequest request = new GroupKeyRequest("requestId", "streamId", "publicKey", [key.getGroupKeyId()])

        when:
        msgCreationUtil.createGroupKeyResponse(TestingAddresses.SUBSCRIBER_ID, request, [key])

        then:
        thrown SigningRequiredException
    }

    void "createGroupKeyResponse() creates correct group key response"() {
        GroupKey groupKey = GroupKey.generate()
        GroupKeyRequest request = new GroupKeyRequest("requestId", "streamId", encryptionUtil.publicKeyAsPemString, [groupKey.getGroupKeyId()])

        when:
        StreamMessage msg = msgCreationUtil.createGroupKeyResponse(TestingAddresses.SUBSCRIBER_ID, request, [groupKey])

        then:
        msg.getStreamId() == KeyExchangeUtil.getKeyExchangeStreamId(TestingAddresses.SUBSCRIBER_ID)
        msg.getMessageType() == StreamMessage.MessageType.GROUP_KEY_RESPONSE
        msg.getEncryptionType() == StreamMessage.EncryptionType.RSA
        msg.getSignature() != null

        when:
        GroupKeyResponse response = (GroupKeyResponse) AbstractGroupKeyMessage.deserialize(msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_RESPONSE)

        then:
        response.getStreamId() == "streamId"
        encryptionUtil.decryptWithPrivateKey(response.getKeys()[0]) == groupKey
    }

    void "createGroupKeyAnnounce() should throw if SigningUtil is not set"() {
        msgCreationUtil = new MessageCreationUtil(TestingAddresses.PUBLISHER_ID, null)
        GroupKey key = GroupKey.generate()

        when:
        msgCreationUtil.createGroupKeyAnnounce(TestingAddresses.SUBSCRIBER_ID, "streamId", "publicKey", [key])
        then:
        thrown SigningRequiredException
    }

    void "createGroupKeyAnnounce() sends the group key announce RSA encrypted on the subscriber's key exchange stream"() {
        GroupKey groupKey = GroupKey.generate()

        when:
        StreamMessage msg = msgCreationUtil.createGroupKeyAnnounce(TestingAddresses.SUBSCRIBER_ID, "streamId", encryptionUtil.publicKeyAsPemString, [groupKey])

        then:
        msg.getStreamId() == KeyExchangeUtil.getKeyExchangeStreamId(TestingAddresses.SUBSCRIBER_ID)
        msg.getMessageType() == StreamMessage.MessageType.GROUP_KEY_ANNOUNCE
        msg.getEncryptionType() == StreamMessage.EncryptionType.RSA
        msg.getSignature() != null

        when:
        GroupKeyAnnounce announce = (GroupKeyAnnounce) AbstractGroupKeyMessage.deserialize(msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_ANNOUNCE)

        then:
        announce.getStreamId() == "streamId"
        encryptionUtil.decryptWithPrivateKey(announce.getKeys()[0]) == groupKey
    }

    void "createGroupKeyErrorResponse() should throw if SigningUtil is not set"() {
        msgCreationUtil = new MessageCreationUtil(TestingAddresses.PUBLISHER_ID, null)

        when:
        msgCreationUtil.createGroupKeyErrorResponse(TestingAddresses.SUBSCRIBER_ID, new GroupKeyRequest("requestId", "streamId", "rsaPublicKey" ,["keyId1"]), new Exception())

        then:
        thrown SigningRequiredException
    }

    void "createGroupKeyErrorResponse() creates the correct error message"() {
        when:
        StreamMessage msg = msgCreationUtil.createGroupKeyErrorResponse(TestingAddresses.SUBSCRIBER_ID, new GroupKeyRequest("requestId", "streamId", "publicKey", ["keyId1"]), new InvalidGroupKeyRequestException("some error message"))
        GroupKeyErrorResponse response = (GroupKeyErrorResponse) AbstractGroupKeyMessage.deserialize(msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE)

        then:
        msg.getStreamId() == KeyExchangeUtil.getKeyExchangeStreamId(TestingAddresses.SUBSCRIBER_ID)
        msg.getMessageType() == StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE
        msg.getEncryptionType() == StreamMessage.EncryptionType.NONE
        msg.getSignature() != null

        response.getRequestId() == "requestId"
        response.getStreamId() == "streamId"
        response.getCode() == "INVALID_GROUP_KEY_REQUEST"
        response.getMessage() == "some error message"
    }
}
