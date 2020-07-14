package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class DecryptionKeySequenceSpec extends Specification {
    UnencryptedGroupKey genGroupKey() {
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes))
    }

    StreamMessage genMsg(HashMap<String, Object> content) {
        return new StreamMessageV31("stream-id", 0, 1L, 0L, "publisherId", "msgChainId",
                0L, 0L, StreamMessage.MessageType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
    }

    void "decrypts sequence of StreamMessage using key sequence"() {
        UnencryptedGroupKey k1 = genGroupKey()
        UnencryptedGroupKey k2 = genGroupKey()
        UnencryptedGroupKey k3 = genGroupKey()

        StreamMessage m1 = genMsg([m1: 'm1'])
        EncryptionUtil.encryptStreamMessage(m1, k1.secretKey)
        StreamMessage m2 = genMsg([m2: 'm2'])
        EncryptionUtil.encryptStreamMessage(m2, k2.secretKey)
        StreamMessage m3 = genMsg([m3: 'm3'])
        EncryptionUtil.encryptStreamMessage(m3, k2.secretKey)
        StreamMessage m4 = genMsg([m4: 'm4'])
        EncryptionUtil.encryptStreamMessage(m4, k3.secretKey)
        StreamMessage m5 = genMsg([m5: 'm5'])
        StreamMessage m6 = genMsg([m6: 'm6'])
        EncryptionUtil.encryptStreamMessage(m6, k3.secretKey)

        ArrayList<StreamMessage> msgs = [m1, m2, m3, m4, m5, m6]

        DecryptionKeySequence util = new DecryptionKeySequence((ArrayList<UnencryptedGroupKey>)[k1, k2, k3])
        when:
        for (StreamMessage m: msgs) {
            util.tryToDecryptResent(m)
        }
        then:
        m1.encryptionType == StreamMessage.EncryptionType.NONE
        m1.parsedContent == [m1: 'm1']
        m2.encryptionType == StreamMessage.EncryptionType.NONE
        m2.parsedContent == [m2: 'm2']
        m3.encryptionType == StreamMessage.EncryptionType.NONE
        m3.parsedContent == [m3: 'm3']
        m4.encryptionType == StreamMessage.EncryptionType.NONE
        m4.parsedContent == [m4: 'm4']
        m6.encryptionType == StreamMessage.EncryptionType.NONE
        m6.parsedContent == [m6: 'm6']
    }
}
