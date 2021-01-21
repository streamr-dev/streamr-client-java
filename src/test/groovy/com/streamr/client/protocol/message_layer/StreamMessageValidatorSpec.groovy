package com.streamr.client.protocol.message_layer

import com.streamr.client.exceptions.ValidationException
import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy
import com.streamr.client.rest.Stream
import com.streamr.client.utils.Address
import com.streamr.client.utils.AddressValidityUtil
import com.streamr.client.utils.EncryptionUtil
import com.streamr.client.utils.GroupKey
import com.streamr.client.utils.MessageCreationUtil
import com.streamr.client.utils.SigningUtil
import org.web3j.crypto.ECKeyPair

class StreamMessageValidatorSpec extends StreamrSpecification {
    StreamMessageValidator validator

    final GroupKey groupKey = GroupKey.generate()
    final EncryptionUtil encryptionUtil = new EncryptionUtil()

    final MessageCreationUtil publisherMsgCreationUtil = new MessageCreationUtil(publisher, new SigningUtil(ECKeyPair.create(new BigInteger(publisherPrivateKey, 16))))
    final MessageCreationUtil subscriberMsgCreationUtil = new MessageCreationUtil(subscriber, new SigningUtil(ECKeyPair.create(new BigInteger(subscriberPrivateKey, 16))))

    StreamMessage msgSigned
    StreamMessage groupKeyRequest
    StreamMessage groupKeyResponse
    StreamMessage groupKeyAnnounceRekey
    StreamMessage groupKeyErrorResponse

    String signature = "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    MessageId msgId = new MessageId("streamId", 0, 425235315L, 0L, publisherId, "msgChainId")

    // The signature of this message is invalid but still in a correct format
    StreamMessage msgInvalid = new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
            .withSerializedContent(Json.mapAdapter.toJson([foo: 'bar']))
            .withEncryptionType(StreamMessage.EncryptionType.NONE)
            .withGroupKeyId(null)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .withSignature(signature)
            .createStreamMessage()

    // By checking that this message is verified without throwing, we ensure that the SigningUtil is not called because the signature is not in the correct form
    StreamMessage msgWrongFormat = new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
            .withSerializedContent(Json.mapAdapter.toJson([foo: 'bar']))
            .withEncryptionType(StreamMessage.EncryptionType.NONE)
            .withGroupKeyId(null)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .withSignature("wrong-signature")
            .createStreamMessage()

    StreamMessage msgUnsigned = new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
            .withSerializedContent(Json.mapAdapter.toJson([foo: 'bar']))
            .withEncryptionType(StreamMessage.EncryptionType.NONE)
            .withGroupKeyId(null)
            .withSignatureType(StreamMessage.SignatureType.NONE)
            .withSignature(null)
            .createStreamMessage()

    List<Address> publishers
    List<Address> subscribers
    Stream stream

    AddressValidityUtil addressValidityUtil = new AddressValidityUtil(
            { String id -> subscribers },
            {  String streamId, Address address -> subscribers.contains(address) },
            { String id -> publishers },
            {  String streamId, Address address -> publishers.contains(address) },
    )
    StreamMessageValidator getValidator(SignatureVerificationPolicy verifySignatures) {
        return new StreamMessageValidator({ String id -> stream }, addressValidityUtil, verifySignatures)
    }

    void setup() {
        stream = new Stream.Builder()
                .withName("test-stream")
                .withDescription("")
                .withId("streamId")
                .withPartitions(1)
                .withRequireSignedData(false)
                .withRequireEncryptedData(false)
                .createStream()
        msgSigned = StreamMessage.deserialize('[31,["tagHE6nTQ9SJV2wPoCxBFw",0,1587141844396,0,"0x6807295093ac5da6fb2a10f7dedc5edd620804fb","k000EDTMtqOTLM8sirFj"],[1587141844312,0],27,0,"{\\"eventType\\":\\"trade\\",\\"eventTime\\":1587141844398,\\"symbol\\":\\"ETHBTC\\",\\"tradeId\\":172530352,\\"price\\":0.02415,\\"quantity\\":0.296,\\"buyerOrderId\\":687544144,\\"sellerOrderId\\":687544104,\\"time\\":1587141844396,\\"maker\\":false,\\"ignored\\":true}",2,"0x6ad42041804c34902aaf7f07780b3e468ec2faec84eda2ff504d5fc26377d5556481d133d7f3f112c63cd48ee9081172013fb0ae1a61b45ee9ca89e057b099591b"]')

        groupKeyRequest = subscriberMsgCreationUtil.createGroupKeyRequest(publisher, stream.getId(), encryptionUtil.publicKeyAsPemString, [groupKey.getGroupKeyId()])
        groupKeyResponse = publisherMsgCreationUtil.createGroupKeyResponse(subscriber, (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(groupKeyRequest), [groupKey])
        groupKeyAnnounceRekey = publisherMsgCreationUtil.createGroupKeyAnnounce(subscriber, stream.getId(), encryptionUtil.publicKeyAsPemString, [groupKey])
        groupKeyErrorResponse = publisherMsgCreationUtil.createGroupKeyErrorResponse(subscriber, (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(groupKeyRequest), new Exception("Test exception"))

        validator = getValidator(SignatureVerificationPolicy.ALWAYS)
        publishers = [publisherId, publisher]
        subscribers = [subscriber]
    }

    /*****
     * Validating normal messages
     */

    void "passes validation for valid signatures"() {
        when:
        validator.validate(msgSigned)

        then:
        notThrown(Exception)
    }

    void "can open the caches again after they have been closed (no CacheClosedException)"() {
        when:
        validator.clearAndClose()
        validator.validate(msgSigned)
        then:
        notThrown(Exception)
    }

    void "should return true without verifying if policy is 'never' for both signed and unsigned messages"() {
        validator = getValidator(SignatureVerificationPolicy.NEVER)

        when:
        validator.validate(msgWrongFormat) // Signingvalidator.hasValidSignature() would throw if called
        validator.validate(msgUnsigned)
        validator.validate(msgSigned)
        then:
        notThrown(Exception)
    }

    void "should throw if policy is 'always' and message not signed"() {
        when:
        validator.validate(msgUnsigned)
        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.POLICY_VIOLATION
    }

    void "should throw if the signature is invalid"() {
        when:
        validator.validate(msgInvalid)
        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "should verify if policy is 'auto' and signature is present, even if stream does not require signed data"() {
        validator = getValidator(SignatureVerificationPolicy.AUTO)
        when:
        validator.validate(msgInvalid)
        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "should throw if policy is 'auto', signature is not present, and stream requires signed data"() {
        stream = new Stream.Builder(stream)
                .withRequireSignedData(true)
                .createStream()
        validator = getValidator(SignatureVerificationPolicy.AUTO)
        when:
        validator.validate(msgUnsigned)
        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.POLICY_VIOLATION
    }

    void "should not throw if policy is 'auto', signature is not present, and stream does not require signed data"() {
        validator = getValidator(SignatureVerificationPolicy.AUTO)
        when:
        validator.validate(msgUnsigned)
        then:
        notThrown(Exception)
    }

    void "accepts valid encrypted messages"() {
        stream = new Stream.Builder(stream)
                .withRequireEncryptedData(true)
                .createStream()
        msgSigned = new StreamMessage.Builder(msgSigned)
                .withEncryptionType(StreamMessage.EncryptionType.AES)
                .createStreamMessage()

        when:
        validator.validate(msgSigned)
        then:
        notThrown(Exception)
    }

    void "rejects unencrypted messages if encryption is required"() {
        stream = new Stream.Builder(stream)
                .withRequireEncryptedData(true)
                .createStream()
        msgSigned = new StreamMessage.Builder(msgSigned)
                .withEncryptionType(StreamMessage.EncryptionType.NONE)
                .createStreamMessage()

        when:
        validator.validate(msgSigned)
        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.POLICY_VIOLATION
    }

    /**
     * Validating GroupKeyRequests
     */

    void "[GroupKeyRequest] accepts valid"() {
        when:
        validator.validate(groupKeyRequest)

        then:
        notThrown(Exception)
    }

    void "[GroupKeyRequest] rejects unsigned"() {
        groupKeyRequest = new StreamMessage.Builder(groupKeyRequest)
                .withSignature(null)
                .withSignatureType(StreamMessage.SignatureType.NONE)
                .createStreamMessage()

        when:
        validator.validate(groupKeyRequest)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyRequest] rejects invalid signatures"() {
        def signature = groupKeyRequest.getSignature().replace('a', 'b')
        groupKeyRequest = new StreamMessage.Builder(groupKeyRequest)
                .withSignature(signature)
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        when:
        validator.validate(groupKeyRequest)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "[GroupKeyRequest] rejects messages to invalid publishers"() {
        publishers.remove(publisher)

        when:
        validator.validate(groupKeyRequest)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    void "[GroupKeyRequest] rejects messages from unpermitted subscribers"() {
        subscribers.remove(subscriber)

        when:
        validator.validate(groupKeyRequest)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    /**
     * Validating GroupKeyResponses
     */

    void "[GroupKeyResponse] accepts valid"() {
        when:
        validator.validate(groupKeyResponse)

        then:
        notThrown(Exception)
    }

    void "[GroupKeyResponse] rejects unsigned"() {
        groupKeyResponse = new StreamMessage.Builder(groupKeyResponse)
                .withSignature(null)
                .withSignatureType(StreamMessage.SignatureType.NONE)
                .createStreamMessage()

        when:
        validator.validate(groupKeyResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyResponse] rejects invalid signatures"() {
        def signature = groupKeyResponse.getSignature().replace('a', 'b')
        groupKeyResponse = new StreamMessage.Builder(groupKeyResponse)
                .withSignature(signature)
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        when:
        validator.validate(groupKeyResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "[GroupKeyResponse] rejects messages from invalid publishers"() {
        publishers.remove(publisher)

        when:
        validator.validate(groupKeyResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    void "[GroupKeyResponse] rejects messages to unpermitted subscribers"() {
        subscribers.remove(subscriber)

        when:
        validator.validate(groupKeyResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    /**
     * Validating GroupKeyAnnounce
     */

    void "[GroupKeyAnnounce] accepts valid"() {
        when:
        validator.validate(groupKeyAnnounceRekey)

        then:
        notThrown(Exception)
    }

    void "[GroupKeyAnnounce] rejects unsigned"() {
        groupKeyAnnounceRekey = new StreamMessage.Builder(groupKeyAnnounceRekey)
                .withSignature(null)
                .withSignatureType(StreamMessage.SignatureType.NONE)
                .createStreamMessage()

        when:
        validator.validate(groupKeyAnnounceRekey)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyAnnounce] rejects invalid signatures"() {
        def signature = groupKeyAnnounceRekey.getSignature().replace('a', 'b')
        groupKeyAnnounceRekey = new StreamMessage.Builder(groupKeyAnnounceRekey)
                .withSignature(signature)
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        when:
        validator.validate(groupKeyAnnounceRekey)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "[GroupKeyAnnounce] rejects messages from invalid publishers"() {
        publishers.remove(publisher)

        when:
        validator.validate(groupKeyAnnounceRekey)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    void "[GroupKeyAnnounce] rejects messages to unpermitted subscribers"() {
        subscribers.remove(subscriber)

        when:
        validator.validate(groupKeyAnnounceRekey)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    /**
     * Validating GroupKeyErrorResponses
     */

    void "[GroupKeyErrorResponse] accepts valid"() {
        when:
        validator.validate(groupKeyErrorResponse)

        then:
        notThrown(Exception)
    }

    void "[GroupKeyErrorResponse] rejects unsigned"() {
        groupKeyErrorResponse = new StreamMessage.Builder(groupKeyErrorResponse)
                .withSignature(null)
                .withSignatureType(StreamMessage.SignatureType.NONE)
                .createStreamMessage()

        when:
        validator.validate(groupKeyErrorResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyErrorResponse] rejects invalid signatures"() {
        def signature = groupKeyErrorResponse.getSignature().replace('a', 'b')
        groupKeyErrorResponse = new StreamMessage.Builder(groupKeyErrorResponse)
                .withSignature(signature)
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        when:
        validator.validate(groupKeyErrorResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "[GroupKeyErrorResponse] rejects messages from invalid publishers"() {
        publishers.remove(publisher)

        when:
        validator.validate(groupKeyErrorResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    void "[GroupKeyErrorResponse] rejects messages to unpermitted subscribers"() {
        subscribers.remove(subscriber)

        when:
        validator.validate(groupKeyErrorResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

}
