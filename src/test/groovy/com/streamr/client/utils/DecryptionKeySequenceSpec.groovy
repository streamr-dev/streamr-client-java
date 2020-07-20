package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.MessageRef
import com.streamr.client.protocol.message_layer.StreamMessage
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class DecryptionKeySequenceSpec extends Specification {
    GroupKey genGroupKey() {
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        return new GroupKey(Hex.encodeHexString(keyBytes))
    }

    StreamMessage genMsg(HashMap<String, Object> content) {
        return new StreamMessage(
                new MessageID("stream-id", 0, 1L, 0L, "publisherId", "msgChainId"),
                new MessageRef(0L, 0L),
                content)
    }

    void "decrypts sequence of StreamMessage using key sequence"() {
        GroupKey k1 = genGroupKey()
        GroupKey k2 = genGroupKey()
        GroupKey k3 = genGroupKey()

        StreamMessage m1 = genMsg([m1: 'm1'])
        EncryptionUtil.encryptStreamMessage(m1, k1.toSecretKey)
        StreamMessage m2 = genMsg([m2: 'm2'])
        EncryptionUtil.encryptStreamMessage(m2, k2.toSecretKey)
        StreamMessage m3 = genMsg([m3: 'm3'])
        EncryptionUtil.encryptStreamMessage(m3, k2.toSecretKey)
        StreamMessage m4 = genMsg([m4: 'm4'])
        EncryptionUtil.encryptStreamMessage(m4, k3.toSecretKey)
        StreamMessage m5 = genMsg([m5: 'm5'])
        StreamMessage m6 = genMsg([m6: 'm6'])
        EncryptionUtil.encryptStreamMessage(m6, k3.toSecretKey)

        ArrayList<StreamMessage> msgs = [m1, m2, m3, m4, m5, m6]

        DecryptionKeySequence util = new DecryptionKeySequence((ArrayList<GroupKey>)[k1, k2, k3])
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
