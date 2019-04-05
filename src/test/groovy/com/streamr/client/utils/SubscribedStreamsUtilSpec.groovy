package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidSignatureException
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import com.streamr.client.rest.Stream
import spock.lang.Specification
import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy

class SubscribedStreamsUtilSpec extends Specification {
    String signature = "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    MessageID msgId = new MessageID("streamId", 0, 425235315L, 0L, "publisherId", "msgChainId")

    // The signature of this message is invalid but still in a correct format
    StreamMessage msgInvalid = new StreamMessageV30(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, signature)

    // By checking that this message is verified without throwing, we ensure that the SigningUtil is not called because the signature is not in the correct form
    StreamMessage msgWrongFormat = new StreamMessageV30(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "wrong-signature")

    StreamMessage msgUnsigned = new StreamMessageV30(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

    List<String> publishers = ["publisherId"]
    Stream stream = new Stream("test-stream", "")
    SubscribedStreamsUtil getUtil(SignatureVerificationPolicy verifySignatures) {
        return new SubscribedStreamsUtil({ String id -> stream }, { String id -> publishers }, verifySignatures)
    }

    void "should return true without verifying if policy is 'never' for both signed and unsigned messages"() {
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.NEVER)
        when:
        util.verifyStreamMessage(msgWrongFormat) // SigningUtil.hasValidSignature() would throw if called
        util.verifyStreamMessage(msgUnsigned)
        then:
        notThrown(InvalidSignatureException)
    }

    void "should throw if policy is 'always' and message not signed"() {
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.ALWAYS)
        when:
        util.verifyStreamMessage(msgUnsigned)
        then:
        thrown(InvalidSignatureException)
    }

    void "should verify if policy is 'always'"() {
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.ALWAYS)
        when:
        util.verifyStreamMessage(msgInvalid)
        then:
        thrown(InvalidSignatureException)
    }

    void "should verify if policy is 'auto' and signature is present, even if stream does not require signed data"() {
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.verifyStreamMessage(msgInvalid)
        then:
        thrown(InvalidSignatureException)
    }

    void "should throw if policy is 'auto', signature is not present, and stream requires signed data"() {
        stream.setRequireSignedData(true)
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.verifyStreamMessage(msgUnsigned)
        then:
        thrown(InvalidSignatureException)
        stream.setRequireSignedData(false)
    }

    void "should not throw if policy is 'auto', signature is not present, and stream does not require signed data"() {
        SubscribedStreamsUtil util = getUtil(SignatureVerificationPolicy.AUTO)
        when:
        util.verifyStreamMessage(msgUnsigned)
        then:
        notThrown(InvalidSignatureException)
    }
}
