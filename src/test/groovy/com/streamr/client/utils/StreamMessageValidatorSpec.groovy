package com.streamr.client.utils

import com.streamr.client.exceptions.ValidationException
import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import com.streamr.client.rest.Stream
import spock.lang.Specification

class StreamMessageValidatorSpec extends Specification {
    StreamMessageValidator validator

    // publisher private key: d462a6f2ccd995a346a841d110e8c6954930a1c22851c0032d3116d8ccd2296a
    String publisher = "0x6807295093ac5da6fb2a10f7dedc5edd620804fb"
    // subscriber private key: 81fe39ed83c4ab997f64564d0c5a630e34c621ad9bbe51ad2754fac575fc0c46
    String subscriber = "0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527"

    StreamMessage msgSigned
    StreamMessage groupKeyRequest
    StreamMessage groupKeyResponse
    StreamMessage groupKeyReset
    StreamMessage groupKeyErrorResponse

    String signature = "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    MessageID msgId = new MessageID("streamId", 0, 425235315L, 0L, "publisherId", "msgChainId")

    // The signature of this message is invalid but still in a correct format
    StreamMessage msgInvalid = new StreamMessageV31(msgId, null, StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, signature)

    // By checking that this message is verified without throwing, we ensure that the SigningUtil is not called because the signature is not in the correct form
    StreamMessage msgWrongFormat = new StreamMessageV31(msgId, null, StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "wrong-signature")

    StreamMessage msgUnsigned = new StreamMessageV31(msgId, null, StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'],
            StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)

    List<String> publishers
    List<String> subscribers
    Stream stream

    AddressValidityUtil addressValidityUtil = new AddressValidityUtil(
            { String id -> subscribers },
            {  String streamId, address -> subscribers.contains(address) },
            { String id -> publishers },
            {  String streamId, address -> publishers.contains(address) },
    )
    StreamMessageValidator getValidator(SignatureVerificationPolicy verifySignatures) {
        return new StreamMessageValidator({ String id -> stream }, addressValidityUtil, verifySignatures)
    }

    void setup() {
        stream = new Stream("test-stream", "")
        stream.setPartitions(1)
        stream.setRequireSignedData(false)
        stream.setRequireEncryptedData(false)

        msgSigned = StreamMessage.deserialize('[31,["tagHE6nTQ9SJV2wPoCxBFw",0,1587141844396,0,"0x6807295093ac5da6fb2a10f7dedc5edd620804fb","k000EDTMtqOTLM8sirFj"],[1587141844312,0],27,0,"{\\"eventType\\":\\"trade\\",\\"eventTime\\":1587141844398,\\"symbol\\":\\"ETHBTC\\",\\"tradeId\\":172530352,\\"price\\":0.02415,\\"quantity\\":0.296,\\"buyerOrderId\\":687544144,\\"sellerOrderId\\":687544104,\\"time\\":1587141844396,\\"maker\\":false,\\"ignored\\":true}",2,"0x6ad42041804c34902aaf7f07780b3e468ec2faec84eda2ff504d5fc26377d5556481d133d7f3f112c63cd48ee9081172013fb0ae1a61b45ee9ca89e057b099591b"]')
        groupKeyRequest = StreamMessage.deserialize('[31,["SYSTEM/keyexchange/0x6807295093ac5da6fb2a10f7dedc5edd620804fb",0,1587143350864,0,"0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527","2AC1lJgGTPhVzNCr4lyT"],null,28,0,"{\\"requestId\\":\\"groupKeyRequestId\\",\\"streamId\\":\\"tagHE6nTQ9SJV2wPoCxBFw\\",\\"publicKey\\":\\"rsaPublicKey\\",\\"range\\":{\\"start\\":1354155,\\"end\\":2344155}}",2,"0xa442e08c54257f3245abeb9a64c9381b2459029c6f9d88ff3b4839e67843519736b5f469b3d36a5d659f7eb47fb5c4af165445aa176ad01e6134e0901e0f5fd01c"]')
        groupKeyResponse = StreamMessage.deserialize('[31,["SYSTEM/keyexchange/0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527",0,1587143432683,0,"0x6807295093ac5da6fb2a10f7dedc5edd620804fb","2hmxXpkhmaLcJipCDVDm"],null,29,1,"{\\"requestId\\":\\"groupKeyRequestId\\",\\"streamId\\":\\"tagHE6nTQ9SJV2wPoCxBFw\\",\\"keys\\":[{\\"groupKey\\":\\"encrypted-group-key\\",\\"start\\":34524}]}",2,"0xe633ef60a4ad8c80e6d58010614e08376912711261d9136b3debf4c5a602b8e27e7235d58667c470791373e9fa2757575d02f539cf9556a6724661ef28c055871c"]')
        groupKeyReset = StreamMessage.deserialize('[31,["SYSTEM/keyexchange/0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527",0,1587143432683,0,"0x6807295093ac5da6fb2a10f7dedc5edd620804fb","2hmxXpkhmaLcJipCDVDm"],null,30,1,"{\\"streamId\\":\\"tagHE6nTQ9SJV2wPoCxBFw\\",\\"groupKey\\":\\"encrypted-group-key\\",\\"start\\":34524}",2,"0xfcc1b55818ed8949e3d94e423c320ae6fdc732f6956cabec87b0e8e1674a29de0f483aeed14914496ea572d81cfd5eaf232a7d1ccb3cb8b0c0ed9cc6874b880b1b"]')
        groupKeyErrorResponse = StreamMessage.deserialize('[31,["SYSTEM/keyexchange/0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527",0,1587143432683,0,"0x6807295093ac5da6fb2a10f7dedc5edd620804fb","2hmxXpkhmaLcJipCDVDm"],null,31,1,"{\\"requestId\\":\\"groupKeyRequestId\\",\\"streamId\\":\\"tagHE6nTQ9SJV2wPoCxBFw\\",\\"code\\":\\"TEST_ERROR\\",\\"message\\":\\"Test error message\\"}",2,"0x74301e65c0cb8f553b7aa2e0eeac61aaff918726f6f7699bd05e9201e591cf0c304b5812c28dd2903b394c57dde1c23dae787ec0005d6e2bc1c03edeb7cdbfc41c"]')

        validator = getValidator(SignatureVerificationPolicy.ALWAYS)
        publishers = ["publisherId", publisher]
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
        stream.setRequireSignedData(true)
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
        stream.setRequireEncryptedData(true)
        msgSigned.setEncryptionType(StreamMessage.EncryptionType.AES)

        when:
        validator.validate(msgSigned)
        then:
        notThrown(Exception)
    }

    void "rejects unencrypted messages if encryption is required"() {
        stream.setRequireEncryptedData(true)
        msgSigned.setEncryptionType(StreamMessage.EncryptionType.NONE)

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
        groupKeyRequest.setSignature(null)
        groupKeyRequest.setSignatureType(StreamMessage.SignatureType.SIGNATURE_TYPE_NONE)

        when:
        validator.validate(groupKeyRequest)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyRequest] rejects invalid signatures"() {
        groupKeyRequest.setSignature(groupKeyRequest.getSignature().replace('a', 'b'))

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
        groupKeyResponse.setSignature(null)
        groupKeyResponse.setSignatureType(StreamMessage.SignatureType.SIGNATURE_TYPE_NONE)

        when:
        validator.validate(groupKeyResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyResponse] rejects invalid signatures"() {
        groupKeyResponse.setSignature(groupKeyResponse.getSignature().replace('a', 'b'))

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
     * Validating GroupKeyResets
     */

    void "[GroupKeyReset] accepts valid"() {
        when:
        validator.validate(groupKeyReset)

        then:
        notThrown(Exception)
    }

    void "[GroupKeyReset] rejects unsigned"() {
        groupKeyReset.setSignature(null)
        groupKeyReset.setSignatureType(StreamMessage.SignatureType.SIGNATURE_TYPE_NONE)

        when:
        validator.validate(groupKeyReset)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyReset] rejects invalid signatures"() {
        groupKeyReset.setSignature(groupKeyReset.getSignature().replace('a', 'b'))

        when:
        validator.validate(groupKeyReset)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.INVALID_SIGNATURE
    }

    void "[GroupKeyReset] rejects messages from invalid publishers"() {
        publishers.remove(publisher)

        when:
        validator.validate(groupKeyReset)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.PERMISSION_VIOLATION
    }

    void "[GroupKeyReset] rejects messages to unpermitted subscribers"() {
        subscribers.remove(subscriber)

        when:
        validator.validate(groupKeyReset)

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
        groupKeyErrorResponse.setSignature(null)
        groupKeyErrorResponse.setSignatureType(StreamMessage.SignatureType.SIGNATURE_TYPE_NONE)

        when:
        validator.validate(groupKeyErrorResponse)

        then:
        ValidationException e = thrown(ValidationException)
        e.getReason() == ValidationException.Reason.UNSIGNED_NOT_ALLOWED
    }

    void "[GroupKeyErrorResponse] rejects invalid signatures"() {
        groupKeyErrorResponse.setSignature(groupKeyErrorResponse.getSignature().replace('a', 'b'))

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
