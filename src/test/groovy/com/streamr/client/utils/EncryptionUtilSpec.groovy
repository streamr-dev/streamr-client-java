package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidRSAKeyException
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

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
    void "throws if invalid public key passed to constructor"() {
        when:
        new EncryptionUtil("wrong-format", null)
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA public key in the PEM format."
    }
    void "throws if invalid private key passed to constructor"() {
        String pemKey = EncryptionUtil.exportKeyAsPemString(genKeyPair().public, true)
        when:
        new EncryptionUtil(pemKey, "wrong-format")
        then:
        InvalidRSAKeyException e = thrown InvalidRSAKeyException
        e.message == "Must be a valid RSA private key in the PEM format."
    }
    void "does not throw when valid keys passed to constructor"() {
        KeyPair keyPair = genKeyPair()
        String publicKey = EncryptionUtil.exportKeyAsPemString(keyPair.public, true)
        String privateKey = EncryptionUtil.exportKeyAsPemString(keyPair.private, false)
        when:
        new EncryptionUtil(publicKey, privateKey)
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
