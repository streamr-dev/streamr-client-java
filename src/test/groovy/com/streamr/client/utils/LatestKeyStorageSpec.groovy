package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyRequestException
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class LatestKeyStorageSpec extends Specification {
    SecureRandom secureRandom = new SecureRandom()
    GroupKey genKey(int keyLength) {
        return genKey(keyLength, new Date())
    }

    GroupKey genKey(int keyLength, Date start) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new GroupKey(Hex.encodeHexString(keyBytes), start)
    }
    void "hasKey() returns true iff there is a GroupKey for the stream"() {
        GroupKey key = genKey(32)
        when:
        GroupKeyStore util = new LatestKeyStorage(["streamId": key])
        then:
        util.hasKey("streamId")
        !util.hasKey("wrong-streamId")
    }
    void "getLatestKey() returns null when there is no GroupKey for the stream"() {
        when:
        GroupKeyStore util = new LatestKeyStorage(new HashMap<String, GroupKey>())
        then:
        util.getLatestKey("streamId") == null
    }
    void "getLatestKey() returns key passed in constructor"() {
        GroupKey key = genKey(32)
        when:
        GroupKeyStore util = new LatestKeyStorage(["streamId": key])
        then:
        util.getLatestKey("streamId") == key
    }
    void "getLatestKey() returns last key added"() {
        GroupKey key1 = genKey(32)
        GroupKey key2 = genKey(32)
        when:
        GroupKeyStore util = new LatestKeyStorage(new HashMap<String, GroupKey>())
        util.addKey("streamId", key1)
        util.addKey("streamId", key2)
        then:
        util.getLatestKey("streamId") == key2
    }
    void "getKeysBetween() throws an exception"() {
        when:
        GroupKeyStore util = new LatestKeyStorage(new HashMap<String, GroupKey>())
        util.getKeysBetween("wrong-streamId", 0, 1)
        then:
        thrown(InvalidGroupKeyRequestException)
    }
}
