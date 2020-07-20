package com.streamr.client.utils

import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom

class KeyHistoryStorageSpec extends Specification {
    SecureRandom secureRandom = new SecureRandom()
    GroupKey genKey(int keyLength) {
        return genKey(keyLength, new Date())
    }

    GroupKey genKey(int keyLength, Date start) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new GroupKey(Hex.encodeHexString(keyBytes), start)
    }
    void "hasKey() returns true iff there is a GroupKeyHistory for the stream"() {
        GroupKey key = genKey(32)
        when:
        GroupKeyStore util = new KeyHistoryStorage(["streamId": key])
        then:
        util.hasKey("streamId")
        !util.hasKey("wrong-streamId")
    }
    void "addKey() throws if key added is older than latest key"() {
        GroupKey key10 = genKey(32, new Date(10))
        GroupKey key5 = genKey(32, new Date(5))
        GroupKeyStore util = new KeyHistoryStorage(["streamId": key10])
        when:
        util.addKey("streamId", key5)
        then:
        thrown(IllegalArgumentException)
    }
    void "getLatestKey() returns null when there is no GroupKeyHistory for the stream"() {
        when:
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        then:
        util.getLatestKey("streamId") == null
    }
    void "getLatestKey() returns key passed in constructor"() {
        GroupKey key = genKey(32)
        when:
        GroupKeyStore util = new KeyHistoryStorage(["streamId": key])
        then:
        util.getLatestKey("streamId") == key
    }
    void "getLatestKey() returns last key added"() {
        GroupKey key1 = genKey(32)
        GroupKey key2 = genKey(32)
        when:
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        util.addKey("streamId", key1)
        util.addKey("streamId", key2)
        then:
        util.getLatestKey("streamId") == key2
    }
    void "getKeysBetween() returns empty array for wrong streamId"() {
        when:
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        then:
        util.getKeysBetween("wrong-streamId", 0, 1) == []
    }
    void "getKeysBetween() returns empty array when end time is before start of first key"() {
        when:
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        util.addKey("streamId", genKey(32, new Date(10)))
        then:
        util.getKeysBetween("streamId", 1, 9) == []
    }
    void "returns only the latest key when start time is after last key"() {
        when:
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        util.addKey("streamId", genKey(32, new Date(5)))
        GroupKey latest = genKey(32, new Date(10))
        util.addKey("streamId", latest)
        then:
        util.getKeysBetween("streamId", 15, 200) == [latest]
    }
    void "returns keys in interval start-end"() {
        GroupKeyStore util = new KeyHistoryStorage(new HashMap<String, GroupKey>())
        GroupKey key1 = genKey(32, new Date(10))
        GroupKey key2 = genKey(32, new Date(20))
        GroupKey key3 = genKey(32, new Date(30))
        GroupKey key4 = genKey(32, new Date(40))
        GroupKey key5 = genKey(32, new Date(50))
        when:
        util.addKey("streamId", key1)
        util.addKey("streamId", key2)
        util.addKey("streamId", key3)
        util.addKey("streamId", key4)
        util.addKey("streamId", key5)
        then:
        util.getKeysBetween("streamId", 23, 47) == [key2, key3, key4]
        util.getKeysBetween("streamId", 20, 40) == [key2, key3, key4]
    }
}
