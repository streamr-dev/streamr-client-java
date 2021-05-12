package com.streamr.client.utils

import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.stream.EncryptedGroupKey
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import com.streamr.ethereum.common.Address
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import spock.lang.Specification

class SigningUtilSpec extends Specification {
    BigInteger privateKey
    MessageId msgId

    void setup() {
        privateKey = new BigInteger("23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820", 16)
        final ECKeyPair account = ECKeyPair.create(privateKey)
        final String addr = Keys.getAddress(account.getPublicKey());
        final String hex = Numeric.prependHexPrefix(addr)
        assert new Address(hex).toString() == "0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5".toLowerCase()

        msgId = new MessageId.Builder()
                .withStreamId("streamId")
                .withStreamPartition(0)
                .withTimestamp(425235315L)
                .withSequenceNumber(0L)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
    }

    void "should correctly sign arbitrary data"() {
        String payload = "data-to-sign"
        when:
        String signature = SigningUtil.sign(privateKey, payload)
        then:
        signature == "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    }

    void "should correctly sign a StreamMessage with null previous ref"() {
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(null)
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .createStreamMessage()
        String expectedPayload = "streamId04252353150" + TestingAddresses.PUBLISHER_ID + "msgChainId"+'{"foo":"bar"}'
        when:
        msg = SigningUtil.signStreamMessage(privateKey, msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(privateKey, expectedPayload)
    }

    void "should correctly sign a StreamMessage with non-null previous ref"() {
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(new MessageRef(100, 1))
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .createStreamMessage()
        String expectedPayload = "streamId04252353150" + TestingAddresses.PUBLISHER_ID + "msgChainId1001"+'{"foo":"bar"}'
        when:
        msg = SigningUtil.signStreamMessage(privateKey, msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(privateKey, expectedPayload)
    }

    void "should correctly sign a StreamMessage with new group key"() {
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(new MessageRef(100, 1))
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .withNewGroupKey(new EncryptedGroupKey("groupKeyId", "keyHex"))
                .createStreamMessage()
        String expectedPayload = "streamId04252353150" + TestingAddresses.PUBLISHER_ID + "msgChainId1001"+'{"foo":"bar"}'+'["groupKeyId","keyHex"]'
        when:
        msg = SigningUtil.signStreamMessage(privateKey, msg)
        then:
        msg.signatureType == StreamMessage.SignatureType.ETH
        msg.signature == SigningUtil.sign(privateKey, expectedPayload)
    }

    void "returns false if no signature"() {
        when:
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(null)
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .createStreamMessage()
        then:
        !SigningUtil.hasValidSignature(msg)
    }

    void "returns false if wrong signature"() {
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(null)
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .withSignature("0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b")
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        expect:
        !SigningUtil.hasValidSignature(msg)
    }

    void "returns true if correct signature"() {
        MessageId msgId = new MessageId.Builder()
                .withStreamId("streamId")
                .withStreamPartition(0)
                .withTimestamp(425235315L)
                .withSequenceNumber(0L)
                .withPublisherId(new Address("0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5"))
                .withMsgChainId("msgChainId")
                .createMessageId()
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(null)
                .withContent(TestingContent.fromJsonMap([foo: 'bar']))
                .createStreamMessage()
        msg = SigningUtil.signStreamMessage(privateKey, msg)

        expect:
        SigningUtil.hasValidSignature(msg)
    }

    void "returns true for correct signature of publisher address has upper and lower case letters"() {
        Address address1 = new Address("0x752C8dCAC0788759aCB1B4BB7A9103596BEe3e6c")
        MessageId msgId = new MessageId.Builder()
                .withStreamId("ogzCJrTdQGuKQO7nkLd3Rw")
                .withStreamPartition(0)
                .withTimestamp(1567003338767L)
                .withSequenceNumber(2L)
                .withPublisherId(address1)
                .withMsgChainId("kxYyLiSUQO0SRvMx6gA1")
                .createMessageId()
        StreamMessage msg = new StreamMessage.Builder()
                .withMessageId(msgId)
                .withPreviousMessageRef(new MessageRef(1567003338767L, 1L))
                .withContent(TestingContent.fromJsonMap([numero: 86]))
                .withSignature("0xc97f1fbb4f506a53ecb838db59017f687892494a9073315f8a187846865bf8325333315b116f1142921a97e49e3881eced2b176c69f9d60666b98b7641ad11e01b")
                .withSignatureType(StreamMessage.SignatureType.ETH)
                .createStreamMessage()

        expect:
        SigningUtil.hasValidSignature(msg)
    }
}
