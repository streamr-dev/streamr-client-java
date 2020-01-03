package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyException
import com.streamr.client.exceptions.InvalidRSAKeyException
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import javax.crypto.SecretKey
import javax.xml.bind.DatatypeConverter
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class EncryptionUtilSpec extends Specification {
    KeyPair genKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(4096, new SecureRandom())
            return generator.generateKeyPair()
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e)
        }
    }
    String genGroupKeyHex() {
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        return Hex.encodeHexString(keyBytes)
    }
    SecretKey genSecretKey() {
        return EncryptionUtil.getSecretKeyFromHexString(genGroupKeyHex())
    }
    void "rsa decryption after encryption equals the initial plaintext"() {
        EncryptionUtil util = new EncryptionUtil()
        byte[] plaintext = "some random text".getBytes(StandardCharsets.UTF_8)
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(plaintext, util.getPublicKeyAsPemString())
        then:
        util.decryptWithPrivateKey(ciphertext) == plaintext
    }
    void "rsa decryption after encryption equals the initial plaintext (hex string)"() {
        EncryptionUtil util = new EncryptionUtil()
        byte[] plaintext = "some random text".getBytes(StandardCharsets.UTF_8)
        when:
        String ciphertext = EncryptionUtil.encryptWithPublicKey(Hex.encodeHexString(plaintext), util.getPublicKeyAsPemString())
        then:
        util.decryptWithPrivateKey(ciphertext) == plaintext
    }
    void "aes encryption preserves size (plus iv)"() {
        SecretKey key = genSecretKey()
        byte[] plaintext = "some random text".getBytes(StandardCharsets.UTF_8)
        when:
        byte[] ciphertext = DatatypeConverter.parseHexBinary(EncryptionUtil.encrypt(plaintext, key))
        then:
        ciphertext.length == plaintext.length + 16
    }
    void "aes decryption after encryption equals the initial plaintext"() {
        SecretKey key = genSecretKey()
        byte[] plaintext = "some random text".getBytes(StandardCharsets.UTF_8)
        when:
        String ciphertext = EncryptionUtil.encrypt(plaintext, key)
        then:
        EncryptionUtil.decrypt(ciphertext, key) == plaintext
    }
    void "StreamMessage gets encrypted"() {
        SecretKey key = genSecretKey()
        StreamMessage streamMessage = new StreamMessageV31("stream-id", 0, 1L, 0L, "publisherId", "msgChainId",
                0L, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        EncryptionUtil.encryptStreamMessage(streamMessage, key)
        then:
        streamMessage.serializedContent != '{"foo":"bar"}'
        streamMessage.encryptionType == StreamMessage.EncryptionType.AES
    }
    void "StreamMessage decryption after encryption equals the initial StreamMessage"() {
        SecretKey key = genSecretKey()
        StreamMessage streamMessage = new StreamMessageV31("stream-id", 0, 1L, 0L, "publisherId", "msgChainId",
                0L, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        EncryptionUtil.encryptStreamMessage(streamMessage, key)
        SecretKey newKey = EncryptionUtil.decryptStreamMessage(streamMessage, key)
        then:
        streamMessage.serializedContent == '{"foo":"bar"}'
        streamMessage.encryptionType == StreamMessage.EncryptionType.NONE
        newKey == null
    }
    void "StreamMessage gets encrypted with new key"() {
        SecretKey key = genSecretKey()
        StreamMessage streamMessage = new StreamMessageV31("stream-id", 0, 1L, 0L, "publisherId", "msgChainId",
                0L, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        EncryptionUtil.encryptStreamMessageAndNewKey(genGroupKeyHex(), streamMessage, key)
        then:
        streamMessage.serializedContent != '{"foo":"bar"}'
        streamMessage.encryptionType == StreamMessage.EncryptionType.NEW_KEY_AND_AES
    }
    void "StreamMessage decryption after encryption equals the initial StreamMessage (with new key)"() {
        SecretKey key = genSecretKey()
        StreamMessage streamMessage = new StreamMessageV31("stream-id", 0, 1L, 0L, "publisherId", "msgChainId",
                0L, 0L, StreamMessage.ContentType.CONTENT_TYPE_JSON, StreamMessage.EncryptionType.NONE, [foo: 'bar'], StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        String newGroupKeyHex = genGroupKeyHex()
        when:
        EncryptionUtil.encryptStreamMessageAndNewKey(newGroupKeyHex, streamMessage, key)
        SecretKey newKey = EncryptionUtil.decryptStreamMessage(streamMessage, key)
        then:
        streamMessage.serializedContent == '{"foo":"bar"}'
        streamMessage.encryptionType == StreamMessage.EncryptionType.NONE
        Hex.encodeHexString(newKey.getEncoded()) == newGroupKeyHex
    }
    void "does not throw when valid keys passed to constructor"() {
        KeyPair keyPair = genKeyPair()
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
    void "validateGroupKey() throws"() {
        byte[] keyBytes = new byte[30]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        when:
        EncryptionUtil.validateGroupKey(Hex.encodeHexString(keyBytes))
        then:
        InvalidGroupKeyException e = thrown InvalidGroupKeyException
        e.message == "Group key must be 256 bits long, but got a key length of " + (30 * 8) + " bits."
    }
    void "validateGroupKey() does not throw"() {
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        when:
        EncryptionUtil.validateGroupKey(Hex.encodeHexString(keyBytes))
        then:
        noExceptionThrown()
    }
    void "validatePublicKey() throws"() {
        when:
        EncryptionUtil.validatePublicKey("wrong public key")
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA public key in the PEM format."
    }
    void "validatePublicKey() does not throw"() {
        KeyPair keyPair = genKeyPair()
        when:
        EncryptionUtil.validatePublicKey(EncryptionUtil.exportKeyAsPemString(keyPair.public, true))
        then:
        noExceptionThrown()
    }
    void "validatePrivateKey() throws"() {
        when:
        EncryptionUtil.validatePrivateKey("wrong private key")
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA private key in the PEM format."
    }
    void "validatePrivateKey() does not throw"() {
        KeyPair keyPair = genKeyPair()
        when:
        EncryptionUtil.validatePrivateKey(EncryptionUtil.exportKeyAsPemString(keyPair.private, false))
        then:
        noExceptionThrown()
    }
}
