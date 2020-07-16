package com.streamr.client.utils

import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class GroupKeySpec extends Specification {

    void "equals() return true"() {
        Date now = new Date()
        when:
        EncryptedGroupKey k1 = new EncryptedGroupKey("0x123", now)
        EncryptedGroupKey k2 = new EncryptedGroupKey("0x123", now)
        then:
        k1.equals(k2)
    }

    void "equals() returns false (groupKeyHex)"() {
        Date now = new Date()
        when:
        EncryptedGroupKey k1 = new EncryptedGroupKey("0x123", now)
        EncryptedGroupKey k2 = new EncryptedGroupKey("0x123456", now)
        then:
        !k1.equals(k2)
    }

    void "equals() returns false (start)"() {
        when:
        EncryptedGroupKey k1 = new EncryptedGroupKey("0x123", new Date(123))
        EncryptedGroupKey k2 = new EncryptedGroupKey("0x123", new Date(456))
        then:
        !k1.equals(k2)
    }

    void "getEncrypted()"() {
        when:
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        GroupKey k1 = new GroupKey(Hex.encodeHexString(keyBytes))

        EncryptionUtil encryptionUtil = new EncryptionUtil()
        EncryptedGroupKey k2 = k1.getEncrypted(encryptionUtil.publicKeyAsPemString)

        then:
        k1.groupKeyHex != k2.groupKeyHex
        k1.start == k2.start
    }

    void "getDecrypted()"() {
        when:
        byte[] keyBytes = new byte[32]
        SecureRandom secureRandom = new SecureRandom()
        secureRandom.nextBytes(keyBytes)
        EncryptionUtil encryptionUtil = new EncryptionUtil()
        String encryptedKeyString = EncryptionUtil.encryptWithPublicKey(keyBytes, encryptionUtil.getPublicKeyAsPemString())
        EncryptedGroupKey k1 = new EncryptedGroupKey(encryptedKeyString, new Date())

        GroupKey k2 = k1.getDecrypted(encryptionUtil)
        then:
        k2.groupKeyHex != k1.groupKeyHex
        k2.groupKeyHex == Hex.encodeHexString(keyBytes)
        k2.start == k1.start
    }
}
