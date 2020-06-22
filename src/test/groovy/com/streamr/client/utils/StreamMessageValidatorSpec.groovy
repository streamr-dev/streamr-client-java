package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidSignatureException
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import spock.lang.Specification
import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy

class StreamMessageValidatorSpec extends Specification {
    String signature = "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    MessageID msgId = new MessageID("streamId", 0, 425235315L, 0L, "publisherId", "msgChainId")
    MessageID msgId2 = new MessageID("streamId", 0, 425235315L, 0L, "publisherId2", "msgChainId")
    MessageID msgId3 = new MessageID("streamId", 0, 425235315L, 0L, "publisherId3", "msgChainId")

    // The signature of this message is invalid but still in a correct format
    StreamMessage msgInvalid = new StreamMessageV31(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, signature)
    StreamMessage msgInvalid2 = new StreamMessageV31(msgId2, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, signature)
    StreamMessage msgInvalid3 = new StreamMessageV31(msgId3, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, signature)

    // By checking that this message is verified without throwing, we ensure that the SigningUtil is not called because the signature is not in the correct form
    StreamMessage msgWrongFormat = new StreamMessageV31(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "wrong-signature")

    StreamMessage msgUnsigned = new StreamMessageV31(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

    List<String> publishers = ["publisherId"]
    Stream stream = new Stream("test-stream", "")
    AddressValidityUtil addressValidityUtil = new AddressValidityUtil(null, null, { String id -> publishers }, { String s, String p -> p == "publisherId2"})
    StreamMessageValidator getUtil(SignatureVerificationPolicy verifySignatures) {
        return new StreamMessageValidator({ String id -> stream }, addressValidityUtil, verifySignatures)
    }

    void "can open the caches again after they have been closed (no CacheClosedException)"() {
        when:
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.ALWAYS)
        util.clearAndClose()
        util.validate(msgUnsigned)
        then:
        thrown(InvalidSignatureException)
    }

    void "should return true without verifying if policy is 'never' for both signed and unsigned messages"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.NEVER)
        when:
        util.validate(msgWrongFormat) // SigningUtil.hasValidSignature() would throw if called
        util.validate(msgUnsigned)
        then:
        notThrown(InvalidSignatureException)
    }

    void "should throw if policy is 'always' and message not signed"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.ALWAYS)
        when:
        util.validate(msgUnsigned)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        !e.failedBecauseInvalidPublisher()
    }

    void "should verify if policy is 'always'"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.ALWAYS)
        when:
        util.validate(msgInvalid)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        !e.failedBecauseInvalidPublisher()
    }

    void "should verify if policy is 'auto' and signature is present, even if stream does not require signed data"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.validate(msgInvalid)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        !e.failedBecauseInvalidPublisher()
    }

    void "should throw if policy is 'auto', signature is not present, and stream requires signed data"() {
        stream.setRequireSignedData(true)
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.validate(msgUnsigned)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        !e.failedBecauseInvalidPublisher()
        stream.setRequireSignedData(false)
    }

    void "should not throw if policy is 'auto', signature is not present, and stream does not require signed data"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.validate(msgUnsigned)
        then:
        notThrown(InvalidSignatureException)
    }

    void "should throw but not because the publisher is invalid (publisher not in initial cache)"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.validate(msgInvalid2)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        !e.failedBecauseInvalidPublisher()
    }

    void "should throw because the publisher is invalid (publisher not in initial cache)"() {
        StreamMessageValidator util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.validate(msgInvalid3)
        then:
        InvalidSignatureException e = thrown(InvalidSignatureException)
        e.failedBecauseInvalidPublisher()
    }
}
