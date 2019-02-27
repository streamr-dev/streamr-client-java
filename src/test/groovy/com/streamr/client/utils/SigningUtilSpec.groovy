package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV30
import org.apache.commons.codec.binary.Hex
import org.ethereum.crypto.ECKey
import spock.lang.Specification

class SigningUtilSpec extends Specification {
    ECKey account
    SigningUtil signingUtil

    void setup() {
        String withoutPrefix = "23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820"
        account = ECKey.fromPrivate(new BigInteger(withoutPrefix, 16))
        signingUtil = new SigningUtil(account)
    }

    void "should correctly sign arbitrary data"() {
        String payload = "data-to-sign"
        when:
        String signature = SigningUtil.sign(payload, account)
        then:
        "0x" + Hex.encodeHexString(account.getAddress()).toLowerCase() == "0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5".toLowerCase()
        signature == "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b"
    }

    void "should correctly sign a StreamMessage"() {
        MessageID msgId = new MessageID("streamId", 0, 425235315L, 0L, "publisherId", "msgChainId")
        StreamMessage unsigned = new StreamMessageV30(msgId, null, StreamMessage.ContentType.CONTENT_TYPE_JSON, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        String expectedPayload = "streamId04252353150publisherIdmsgChainId"+'{"foo":"bar"}'
        when:
        StreamMessageV30 signed = (StreamMessageV30) signingUtil.getSignedStreamMessage(unsigned)
        then:
        signed.getMessageID() == msgId
        signed.previousMessageRef == null
        signed.contentType == unsigned.contentType
        signed.content == unsigned.content
        signed.signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH
        signed.signature == SigningUtil.sign(expectedPayload, account)
    }


}