package com.streamr.client.utils

import com.streamr.client.protocol.StreamrSpecification
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import org.apache.commons.codec.binary.Hex
import org.ethereum.crypto.ECKey

class SigningUtilSpec extends StreamrSpecification {
    ECKey account
    Address address
    SigningUtil signingUtil
    MessageID msgId

    void setup() {
        // The EthereumAuthenticationMethod accepts a private key with or without the '0x' prefix. It is removed if present to work with ECKey.fromPrivate.
        // Since we are testing an internal component (SigningUtil), the private key is without prefix.
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        address = new Address("0x" + Hex.encodeHexString(account.getAddress()))
        assert address.toString().toLowerCase() == "0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5".toLowerCase()

        signingUtil = new SigningUtil(account)
        msgId = new MessageID("streamId", 0, 425235315L, 0L, publisherId, "msgChainId")
    }

    void "should correctly sign arbitrary data"() {
        String payload = "data-to-sign"
        when:
        String signature = SigningUtil.sign(payload, account)
        then:
        signature == "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    }

    void "should correctly sign a StreamMessage with null previous ref"() {
        StreamMessage msg = new StreamMessage(msgId, null, [foo: 'bar'])
        String expectedPayload = "streamId04252353150" + publisherId.toLowerCaseString() + "msgChainId"+'{"foo":"bar"}'
        when:
        signingUtil.signStreamMessage(msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(expectedPayload, account)
    }

    void "should correctly sign a StreamMessage with non-null previous ref"() {
        StreamMessage msg = new StreamMessage(msgId, new MessageRef(100, 1), [foo: 'bar'])
        String expectedPayload = "streamId04252353150" + publisherId.toLowerCaseString() + "msgChainId1001"+'{"foo":"bar"}'
        when:
        signingUtil.signStreamMessage(msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(expectedPayload, account)
    }

    void "should correctly sign a StreamMessage with new group key"() {
        StreamMessage msg = new StreamMessage(msgId, new MessageRef(100, 1), [foo: 'bar'])
        msg.setNewGroupKey(new EncryptedGroupKey("groupKeyId", "keyHex"))
        String expectedPayload = "streamId04252353150" + publisherId.toLowerCaseString() + "msgChainId1001"+'{"foo":"bar"}'+'["groupKeyId","keyHex"]'
        when:
        signingUtil.signStreamMessage(msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(expectedPayload, account)
    }

    void "returns false if no signature"() {
        when:
        StreamMessage msg = new StreamMessage(msgId, null, [foo: 'bar'])
        then:
        !SigningUtil.hasValidSignature(msg)
    }

    void "returns false if wrong signature"() {
        StreamMessage msg = new StreamMessage(msgId, null, [foo: 'bar'])
        msg.setSignatureFields("0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b", StreamMessage.SignatureType.ETH)

        expect:
        !SigningUtil.hasValidSignature(msg)
    }

    void "returns true if correct signature"() {
        MessageID msgId = new MessageID("streamId", 0, 425235315L, 0L, address, "msgChainId")
        StreamMessage msg = new StreamMessage(msgId, null, [foo: 'bar'])
        signingUtil.signStreamMessage(msg)

        expect:
        SigningUtil.hasValidSignature(msg)
    }

    void "returns true for correct signature of publisher address has upper and lower case letters"() {
        Address address1 = new Address("0x752C8dCAC0788759aCB1B4BB7A9103596BEe3e6c")
        MessageID msgId = new MessageID("ogzCJrTdQGuKQO7nkLd3Rw", 0, 1567003338767L, 2L, address1, "kxYyLiSUQO0SRvMx6gA1")
        StreamMessage msg = new StreamMessage(msgId, new MessageRef(1567003338767L,1L), [numero: 86])
        msg.setSignatureFields("0xc97f1fbb4f506a53ecb838db59017f687892494a9073315f8a187846865bf8325333315b116f1142921a97e49e3881eced2b176c69f9d60666b98b7641ad11e01b", StreamMessage.SignatureType.ETH)

        expect:
        SigningUtil.hasValidSignature(msg)
    }
}
