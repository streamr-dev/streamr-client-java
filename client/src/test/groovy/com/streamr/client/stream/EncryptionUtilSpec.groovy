package com.streamr.client.stream

import com.streamr.client.crypto.KeysRsa
import com.streamr.client.crypto.RsaKeyPair
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.testing.TestingAddresses
import com.streamr.client.testing.TestingContent
import java.nio.charset.StandardCharsets
import org.web3j.utils.Numeric
import spock.lang.Specification

class EncryptionUtilSpec extends Specification {
    final Map<String, Object> plaintextContent = [foo: 'bar']
    final String serializedPlaintextContent = "{\"foo\":\"bar\"}"
    final byte[] plaintextBytes = "some random text".getBytes(StandardCharsets.UTF_8)

    StreamMessage streamMessage
	GroupKey key
    RsaKeyPair rsaKeyPair
    def setup() {
        def messageId = new MessageId.Builder()
                .withStreamId("stream-id")
                .withStreamPartition(0)
                .withTimestamp(1L)
                .withSequenceNumber(0L)
                .withPublisherId(TestingAddresses.PUBLISHER_ID)
                .withMsgChainId("msgChainId")
                .createMessageId()
        streamMessage = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(new MessageRef(0L, 0L))
                .withContent(TestingContent.fromJsonMap(plaintextContent))
                .createStreamMessage()
        rsaKeyPair = RsaKeyPair.generateKeyPair()
        key = GroupKey.generate()
    }

    void "rsa decryption after encryption equals the initial plaintext"() {
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(plaintextBytes, KeysRsa.exportPublicKeyAsPemString(rsaKeyPair.getRsaPublicKey()))
        then:
        EncryptionUtil.decryptWithPrivateKey(rsaKeyPair.getRsaPrivateKey(), ciphertext) == plaintextBytes
    }

    void "rsa decryption after encryption equals the initial plaintext (hex string)"() {
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(plaintextBytes, KeysRsa.exportPublicKeyAsPemString(rsaKeyPair.getRsaPublicKey()))
        then:
        EncryptionUtil.decryptWithPrivateKey(rsaKeyPair.getRsaPrivateKey(), ciphertext) == plaintextBytes
    }

    void "rsa decryption after encryption equals the initial plaintext (GroupKey)"() {
        when:
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptWithPublicKey(key, rsaKeyPair.getRsaPublicKey())
        then:
        encryptedKey.getGroupKeyId() == key.getGroupKeyId()
        encryptedKey.getEncryptedGroupKeyHex() != key.getGroupKeyHex()

        when:
        GroupKey original = EncryptionUtil.decryptWithPrivateKey(this.rsaKeyPair.getRsaPrivateKey(), encryptedKey)
        then:
        original == key
    }

    void "aes encryption preserves size (plus iv)"() {
        when:
        byte[] ciphertext = Numeric.hexStringToByteArray(EncryptionUtil.encrypt(plaintextBytes, key))

        then:
        ciphertext.length == plaintextBytes.length + 16
    }
    void "multiple same encrypt() calls use differents ivs and produce different ciphertexts"() {
        when:
        String ciphertext1 = EncryptionUtil.encrypt(plaintextBytes, key)
        String ciphertext2 = EncryptionUtil.encrypt(plaintextBytes, key)

        then:
        ciphertext1.substring(0, 32) != ciphertext2.substring(0, 32)
        ciphertext1.substring(32) != ciphertext2.substring(32)
    }
    void "aes decryption after encryption equals the initial plaintext"() {
        when:
        String ciphertext = EncryptionUtil.encrypt(plaintextBytes, key)

        then:
        EncryptionUtil.decrypt(ciphertext, key) == plaintextBytes
    }
    void "encryptStreamMessage() encrypts the message"() {
        when:
        streamMessage = EncryptionUtil.encryptStreamMessage(streamMessage, key)

        then:
        streamMessage.serializedContent != serializedPlaintextContent
        streamMessage.encryptionType == StreamMessage.EncryptionType.AES
    }
    void "encryptStreamMessage, then decryptStreamMessage() equals original message "() {
        when:
        streamMessage = EncryptionUtil.encryptStreamMessage(streamMessage, key)
        streamMessage = EncryptionUtil.decryptStreamMessage(streamMessage, key)

        then:
        streamMessage.serializedContent == serializedPlaintextContent
        streamMessage.parsedContent == plaintextContent
        streamMessage.encryptionType == StreamMessage.EncryptionType.NONE
    }
    void "encryptGroupKey() encrypts the GroupKey"() {
        GroupKey keyToEncrypt = GroupKey.generate()
        GroupKey keyToEncryptWith = key

        when:
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptGroupKey(keyToEncrypt, keyToEncryptWith)
        then:
        encryptedKey.getGroupKeyId() == keyToEncrypt.getGroupKeyId()
        encryptedKey.getEncryptedGroupKeyHex() != keyToEncrypt.getGroupKeyHex()
        encryptedKey.getEncryptedGroupKeyHex() != keyToEncryptWith.getGroupKeyHex()

        when:
        GroupKey original = EncryptionUtil.decryptGroupKey(encryptedKey, keyToEncryptWith)
        then:
        original == keyToEncrypt
    }
}
