package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyRequestException
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class LatestKeyStorageSpec extends Specification {
    SecureRandom secureRandom = new SecureRandom()
    UnencryptedGroupKey genKey(int keyLength) {
        return genKey(keyLength, new Date())
    }

    UnencryptedGroupKey genKey(int keyLength, Date start) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes), start)
    }
    void "hasKey() returns true iff there is a GroupKey for the stream"() {
        UnencryptedGroupKey key = genKey(32)
        when:
        KeyStorage util = new LatestKeyStorage(["streamId": key])
        then:
        util.hasKey("streamId")
        !util.hasKey("wrong-streamId")
    }
    void "getLatestKey() returns null when there is no GroupKey for the stream"() {
        when:
        KeyStorage util = new LatestKeyStorage(new HashMap<String, UnencryptedGroupKey>())
        then:
        util.getLatestKey("streamId") == null
    }
    void "getLatestKey() returns key passed in constructor"() {
        UnencryptedGroupKey key = genKey(32)
        when:
        KeyStorage util = new LatestKeyStorage(["streamId": key])
        then:
        util.getLatestKey("streamId") == key
    }
    void "getLatestKey() returns last key added"() {
        UnencryptedGroupKey key1 = genKey(32)
        UnencryptedGroupKey key2 = genKey(32)
        when:
        KeyStorage util = new LatestKeyStorage(new HashMap<String, UnencryptedGroupKey>())
        util.addKey("streamId", key1)
        util.addKey("streamId", key2)
        then:
        util.getLatestKey("streamId") == key2
    }
    void "getKeysBetween() throws an exception"() {
        when:
        KeyStorage util = new LatestKeyStorage(new HashMap<String, UnencryptedGroupKey>())
        util.getKeysBetween("wrong-streamId", 0, 1)
        then:
        thrown(InvalidGroupKeyRequestException)
    }
}
