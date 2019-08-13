package com.streamr.client.utils

import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

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
}
