package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyException
import com.streamr.client.exceptions.InvalidRSAKeyException
import com.streamr.client.protocol.common.MessageRef
import com.streamr.client.protocol.message_layer.MessageId
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamrSpecification
import com.streamr.client.testing.TestingJson
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.xml.bind.DatatypeConverter
import org.web3j.utils.Numeric

class EncryptionUtilSpec extends StreamrSpecification {

    final Map plaintextContent = [foo: 'bar']
    final String serializedPlaintextContent = "{\"foo\":\"bar\"}"
    final byte[] plaintextBytes = "some random text".getBytes(StandardCharsets.UTF_8)

    StreamMessage streamMessage
    EncryptionUtil util
    GroupKey key

    def setup() {
        def messageId = new MessageId.Builder()
                .withStreamId("stream-id")
                .withStreamPartition(0)
                .withTimestamp(1L)
                .withSequenceNumber(0L)
                .withPublisherId(publisherId)
                .withMsgChainId("msgChainId")
                .createMessageId()
        streamMessage = new StreamMessage.Builder()
                .withMessageId(messageId)
                .withPreviousMessageRef(new MessageRef(0L, 0L))
                .withSerializedContent(TestingJson.toJson(plaintextContent))
                .createStreamMessage()
        util = new EncryptionUtil()
        key = GroupKey.generate()
    }

    void "rsa decryption after encryption equals the initial plaintext"() {
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(plaintextBytes, util.getPublicKeyAsPemString())
        then:
        util.decryptWithPrivateKey(ciphertext) == plaintextBytes
    }

    void "rsa decryption after encryption equals the initial plaintext (hex string)"() {
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(Numeric.toHexStringNoPrefix(plaintextBytes), util.getPublicKeyAsPemString())
        then:
        util.decryptWithPrivateKey(ciphertext) == plaintextBytes
    }

    void "rsa decryption after encryption equals the initial plaintext (StreamMessage)"() {
        when:
        streamMessage = EncryptionUtil.encryptWithPublicKey(streamMessage, util.getPublicKeyAsPemString())
        then:
        streamMessage.getSerializedContent() != serializedPlaintextContent
        streamMessage.getEncryptionType() == StreamMessage.EncryptionType.RSA

        when:
        streamMessage = util.decryptWithPrivateKey(streamMessage)
        then:
        streamMessage.getSerializedContent() == serializedPlaintextContent
        streamMessage.getParsedContent() == plaintextContent
        streamMessage.getEncryptionType() == StreamMessage.EncryptionType.NONE
    }

    void "rsa decryption after encryption equals the initial plaintext (GroupKey)"() {
        when:
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptWithPublicKey(key, util.getPublicKey())
        then:
        encryptedKey.getGroupKeyId() == key.getGroupKeyId()
        encryptedKey.getEncryptedGroupKeyHex() != key.getGroupKeyHex()

        when:
        GroupKey original = util.decryptWithPrivateKey(encryptedKey)
        then:
        original == key
    }

    void "aes encryption preserves size (plus iv)"() {
        when:
        byte[] ciphertext = DatatypeConverter.parseHexBinary(EncryptionUtil.encrypt(plaintextBytes, key))

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

    void "does not throw when valid keys passed to constructor"() {
        KeyPair keyPair = EncryptionUtil.generateKeyPair()

        when:
        new EncryptionUtil((RSAPublicKey) keyPair.public, (RSAPrivateKey) keyPair.private)
        then:
        noExceptionThrown()
    }
    void "does not throw when both params are null (auto key generation)"() {
        when:
        new EncryptionUtil()
        then:
        noExceptionThrown()
    }
    void "validateGroupKey() throws on invalid key length"() {
        byte[] keyBytes = new byte[30]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)

        when:
        EncryptionUtil.validateGroupKey(Numeric.toHexStringNoPrefix(keyBytes))
        then:
        InvalidGroupKeyException e = thrown InvalidGroupKeyException
        e.message == "Group key must be 256 bits long, but got a key length of " + (30 * 8) + " bits."
    }
    void "validateGroupKey() does not throw on correct key length"() {
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        when:
        EncryptionUtil.validateGroupKey(Numeric.toHexStringNoPrefix(keyBytes))
        then:
        noExceptionThrown()
    }
    void "validatePublicKey() throws on invalid key"() {
        when:
        EncryptionUtil.validatePublicKey("wrong public key")
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA public key in the PEM format."
    }
    void "validatePublicKey() does not throw on valid key"() {
        KeyPair keyPair = EncryptionUtil.generateKeyPair()
        when:
        EncryptionUtil.validatePublicKey(EncryptionUtil.exportKeyAsPemString(keyPair.public, true))
        then:
        noExceptionThrown()
    }
    void "validatePrivateKey() throws on invalid private key"() {
        when:
        EncryptionUtil.validatePrivateKey("wrong private key")
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA private key in the PEM format."
    }
    void "validatePrivateKey() does not throw on valid private key"() {
        KeyPair keyPair = EncryptionUtil.generateKeyPair()
        when:
        EncryptionUtil.validatePrivateKey(EncryptionUtil.exportKeyAsPemString(keyPair.private, false))
        then:
        noExceptionThrown()
    }
}
