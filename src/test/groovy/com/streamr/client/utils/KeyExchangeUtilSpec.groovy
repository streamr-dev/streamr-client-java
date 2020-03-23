package com.streamr.client.utils

import com.streamr.client.exceptions.InvalidGroupKeyRequestException
import com.streamr.client.exceptions.InvalidGroupKeyResetException
import com.streamr.client.exceptions.InvalidGroupKeyResponseException
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamMessageV31
import org.apache.commons.codec.binary.Hex
import spock.lang.Specification

import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer
import java.util.function.Function

class KeyExchangeUtilSpec extends Specification {
    SecureRandom secureRandom = new SecureRandom()
    UnencryptedGroupKey genKey(int keyLength) {
        return genKey(keyLength, new Date())
    }

    UnencryptedGroupKey genKey(int keyLength, Date start) {
        byte[] keyBytes = new byte[keyLength]
        secureRandom.nextBytes(keyBytes)
        return new UnencryptedGroupKey(Hex.encodeHexString(keyBytes), start)
    }
    KeyExchangeUtil util
    KeyStorage storage
    MessageCreationUtil messageCreationUtil
    Consumer<StreamMessage> publish
    KeyExchangeUtil.SetGroupKeysFunction setGroupKeysFunction
    ArrayList<StreamMessage> published
    StreamMessage response = new StreamMessageV31(new MessageID("subscriberId", 0, 5145, 0, "publisherId", ""), null,
            StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE, StreamMessage.EncryptionType.RSA, "response", StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
    EncryptionUtil encryptionUtil = new EncryptionUtil()
    AddressValidityUtil addressValidityUtil = new AddressValidityUtil({ String id -> new ArrayList<>()}, { String s1, String s2 -> s1 == "streamId" && s2 == "subscriberId"},
            { String id -> new ArrayList<>()}, { String s, String p -> true})
    UnencryptedGroupKey received
    void setup() {
        storage = Mock(KeyStorage)
        messageCreationUtil = Mock(MessageCreationUtil)
        published = new ArrayList<>()
        publish = new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                published.add(streamMessage)
            }
        }
        setGroupKeysFunction = new KeyExchangeUtil.SetGroupKeysFunction() {
            @Override
            void apply(String streamId, String publisherId, ArrayList<UnencryptedGroupKey> keys) {
                assert streamId == "streamId"
                assert publisherId == "publisherId"
                assert keys.size() == 1
                received = keys[0]
            }
        }
        util = new KeyExchangeUtil(storage, messageCreationUtil, encryptionUtil, addressValidityUtil, publish, setGroupKeysFunction)
    }
    void "should reject unsigned request"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, "subscriberId", "msgChainId")
        Map<String, Object> content = ["publicKey": "rsa public key", "streamId": "streamId"]
        StreamMessage request = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        util.handleGroupKeyRequest(request)
        then:
        InvalidGroupKeyRequestException e = thrown(InvalidGroupKeyRequestException)
        e.message == "Received unsigned group key request (the public key must be signed to avoid MitM attacks)."
    }
    void "should reject request from invalid subscriber"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, "wrong-subscriberId", "msgChainId")
        Map<String, Object> content = ["publicKey": "rsa public key", "streamId": "streamId"]
        StreamMessage request = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyRequest(request)
        then:
        InvalidGroupKeyRequestException e = thrown(InvalidGroupKeyRequestException)
        e.message == "Received group key request for stream 'streamId' from invalid address 'wrong-subscriberId'"
    }
    void "should reject request for a stream for which the client does not have a group key"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, "subscriberId", "msgChainId")
        Map<String, Object> content = ["publicKey": "rsa public key", "streamId": "streamId"]
        StreamMessage request = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyRequest(request)
        then:
        1 * storage.getLatestKey("streamId") >> null
        InvalidGroupKeyRequestException e = thrown(InvalidGroupKeyRequestException)
        e.message == "Received group key request for stream 'streamId' but no group key is set"
    }
    void "should send group key response (latest key)"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, "subscriberId", "msgChainId")
        Map<String, Object> content = ["publicKey": encryptionUtil.publicKeyAsPemString, "streamId": "streamId"]
        StreamMessage request = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        UnencryptedGroupKey key = genKey(32, new Date(123))
        when:
        util.handleGroupKeyRequest(request)
        then:
        1 * storage.getLatestKey("streamId") >> key
        1 * messageCreationUtil.createGroupKeyResponse(*_) >> { arguments ->
            assert arguments[0] == "subscriberId"
            assert arguments[1] == "streamId"
            ArrayList<EncryptedGroupKey> keys = arguments[2]
            assert keys.size() == 1
            EncryptedGroupKey received = keys[0]
            assert Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(received.groupKeyHex)) == key.groupKeyHex
            assert received.startTime == key.startTime
            return response
        }
        published.size() == 1
        published.get(0) == response
    }
    void "should send group key response (range of keys)"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, "subscriberId", "msgChainId")
        // Need to use Double because Moshi parser converts all JSON numbers to double
        Map<String, Object> content = ["publicKey": encryptionUtil.publicKeyAsPemString, "streamId": "streamId", "range": ["start": new Double(123), "end": new Double(456)]]
        StreamMessage request = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        UnencryptedGroupKey key1 = genKey(32, new Date(123))
        UnencryptedGroupKey key2 = genKey(32, new Date(300))
        when:
        util.handleGroupKeyRequest(request)
        then:
        1 * storage.getKeysBetween("streamId", 123L, 456L) >> [key1, key2]
        1 * messageCreationUtil.createGroupKeyResponse(*_) >> { arguments ->
            assert arguments[0] == "subscriberId"
            assert arguments[1] == "streamId"
            ArrayList<EncryptedGroupKey> keys = arguments[2]
            assert keys.size() ==2
            EncryptedGroupKey received1 = keys[0]
            assert Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(received1.groupKeyHex)) == key1.groupKeyHex
            assert received1.startTime == key1.startTime
            EncryptedGroupKey received2 = keys[1]
            assert Hex.encodeHexString(encryptionUtil.decryptWithPrivateKey(received2.groupKeyHex)) == key2.groupKeyHex
            assert received2.startTime == key2.startTime
            return response
        }
        published.size() == 1
        published.get(0) == response
    }
    void "should reject unsigned response"() {
        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        Map<String, Object> content = ["keys": [], "streamId": "streamId"]
        StreamMessage response = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        util.handleGroupKeyResponse(response)
        then:
        InvalidGroupKeyResponseException e = thrown(InvalidGroupKeyResponseException)
        e.message == "Received unsigned group key response (it must be signed to avoid MitM attacks)."
    }
    void "should reject response with invalid group key"() {
        SecureRandom secureRandom = new SecureRandom()
        byte[] keyBytes = new byte[16]
        secureRandom.nextBytes(keyBytes)
        String groupKeyHex = Hex.encodeHexString(keyBytes)
        String encryptedGroupKeyHex = EncryptionUtil.encryptWithPublicKey(groupKeyHex, encryptionUtil.getPublicKeyAsPemString())

        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        // Need to use Double because the Moshi parser converts all JSON numbers to double
        Map<String, Object> content = ["keys": [["groupKey": encryptedGroupKeyHex, "start": new Double(123)]], "streamId": "streamId"]
        StreamMessage response = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyResponse(response)
        then:
        InvalidGroupKeyResponseException e = thrown(InvalidGroupKeyResponseException)
        e.message == "Group key must be 256 bits long, but got a key length of 128 bits."
    }
    void "should update client options and subscriptions with received group key"() {
        SecureRandom secureRandom = new SecureRandom()
        byte[] keyBytes = new byte[32]
        secureRandom.nextBytes(keyBytes)
        String groupKeyHex = Hex.encodeHexString(keyBytes)
        String encryptedGroupKeyHex = EncryptionUtil.encryptWithPublicKey(groupKeyHex, encryptionUtil.getPublicKeyAsPemString())

        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        // Need to use Double because the Moshi parser converts all JSON numbers to double
        Map<String, Object> content = ["keys": [["groupKey": encryptedGroupKeyHex, "start": new Double(123)]], "streamId": "streamId"]
        StreamMessage response = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyResponse(response)
        then:
        received.groupKeyHex == groupKeyHex
        received.startTime == 123L
    }
    void "should reject unsigned reset"() {
        EncryptedGroupKey encryptedGroupKey = EncryptionUtil.genGroupKey().getEncrypted(encryptionUtil.publicKey)
        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        Map<String, Object> content = ["groupKey": encryptedGroupKey.groupKeyHex, "start": encryptedGroupKey.start.getTime(), "streamId": "streamId"]
        StreamMessage reset = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null)
        when:
        util.handleGroupKeyReset(reset)
        then:
        InvalidGroupKeyResetException e = thrown(InvalidGroupKeyResetException)
        e.message == "Received unsigned group key reset (it must be signed to avoid MitM attacks)."
    }
    void "should reject reset with invalid group key"() {
        SecureRandom secureRandom = new SecureRandom()
        byte[] keyBytes = new byte[16]
        secureRandom.nextBytes(keyBytes)
        String groupKeyHex = Hex.encodeHexString(keyBytes)
        String encryptedGroupKeyHex = EncryptionUtil.encryptWithPublicKey(groupKeyHex, encryptionUtil.getPublicKeyAsPemString())

        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        // Need to use Double because the Moshi parser converts all JSON numbers to double
        Map<String, Object> content = ["groupKey": encryptedGroupKeyHex, "start": new Double(123), "streamId": "streamId"]
        StreamMessage reset = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyReset(reset)
        then:
        InvalidGroupKeyResetException e = thrown(InvalidGroupKeyResetException)
        e.message == "Group key must be 256 bits long, but got a key length of 128 bits."
    }
    void "should update client options and subscriptions with received group key reset"() {
        UnencryptedGroupKey newGroupKey = EncryptionUtil.genGroupKey()
        EncryptedGroupKey encryptedGroupKey = newGroupKey.getEncrypted(encryptionUtil.publicKey)

        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, "publisherId", "msgChainId")
        // Need to use Double because the Moshi parser converts all JSON numbers to double
        Map<String, Object> content = ["groupKey": encryptedGroupKey.groupKeyHex, "start": new Double(123), "streamId": "streamId"]
        StreamMessage reset = new StreamMessageV31(id, null, StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE,
                StreamMessage.EncryptionType.RSA, content, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        when:
        util.handleGroupKeyReset(reset)
        then:
        received.groupKeyHex == newGroupKey.groupKeyHex
        received.startTime == 123L
    }

    void "should not revoke if checked recently"() {
        int callCount = 0
        AddressValidityUtil addressValidityUtil2 = new AddressValidityUtil(new Function<String, List<String>>() {
            @Override
            List<String> apply(String s) {
                if (s == "streamId") {
                    callCount++
                }
                return new ArrayList<>()
            }
        }, null,null, null)
        util = new KeyExchangeUtil(storage, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, setGroupKeysFunction)
        when:
        boolean res = util.keyRevocationNeeded("streamId")
        then:
        callCount == 1
        !res
        when:
        res = util.keyRevocationNeeded("streamId")
        then:
        callCount == 1 // not enough time elapsed since last call
        !res
    }

    void "should not revoke if enough time elapsed but less than threshold"() {
        AddressValidityUtil addressValidityUtil2 = Mock(AddressValidityUtil)
        Clock clock = Mock(Clock)
        Instant now = Instant.now()
        clock.instant() >> now
        util = new KeyExchangeUtil(storage, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, setGroupKeysFunction, clock)
        when:
        boolean res = util.keyRevocationNeeded("streamId")
        then:
        1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >> 0
        !res
        when:
        res = util.keyRevocationNeeded("streamId")
        then:
        1 * clock.instant() >> now + Duration.ofMinutes(KeyExchangeUtil.REVOCATION_DELAY + 1)
        1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >> KeyExchangeUtil.REVOCATION_THRESHOLD - 1
        !res
    }

    void "should revoke if threshold reached"() {
        AddressValidityUtil addressValidityUtil2 = Mock(AddressValidityUtil)
        util = new KeyExchangeUtil(storage, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, setGroupKeysFunction)
        when:
        boolean res = util.keyRevocationNeeded("streamId")
        then:
        1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >> KeyExchangeUtil.REVOCATION_THRESHOLD
        res
    }

    void "should rekey by sending group key resets"() {
        MessageID id1 = new MessageID("publisherInbox", 0, 414, 0, "subscriberId1", "msgChainId")
        EncryptionUtil encryptionUtil1 = new EncryptionUtil()
        RSAPublicKey pk1 = encryptionUtil1.publicKey
        Map<String, Object> content1 = ["publicKey": EncryptionUtil.exportKeyAsPemString(pk1, true), "streamId": "streamId", "range": ["start": new Double(123), "end": new Double(456)]]
        StreamMessage request1 = new StreamMessageV31(id1, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content1, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        MessageID id2 = new MessageID("publisherInbox", 0, 414, 0, "subscriberId2", "msgChainId")
        RSAPublicKey pk2 = new EncryptionUtil().publicKey
        Map<String, Object> content2 = ["publicKey": EncryptionUtil.exportKeyAsPemString(pk2, true), "streamId": "streamId", "range": ["start": new Double(123), "end": new Double(456)]]
        StreamMessage request2 = new StreamMessageV31(id2, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content2, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        MessageID id3 = new MessageID("publisherInbox", 0, 414, 0, "subscriberId3", "msgChainId")
        EncryptionUtil encryptionUtil3 = new EncryptionUtil()
        RSAPublicKey pk3 = encryptionUtil3.publicKey
        Map<String, Object> content3 = ["publicKey": EncryptionUtil.exportKeyAsPemString(pk3, true), "streamId": "streamId", "range": ["start": new Double(123), "end": new Double(456)]]
        StreamMessage request3 = new StreamMessageV31(id3, null, StreamMessage.ContentType.GROUP_KEY_REQUEST,
                StreamMessage.EncryptionType.NONE, content3, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

        AddressValidityUtil addressValidityUtil2 = Mock(AddressValidityUtil)
        util = new KeyExchangeUtil(storage, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, setGroupKeysFunction)
        UnencryptedGroupKey resetKey1
        UnencryptedGroupKey resetKey3
        StreamMessageV31 reset1 = new StreamMessageV31(new MessageID("subscriberId1", 0, 5145, 0, "publisherId", ""), null,
                StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE, StreamMessage.EncryptionType.RSA, "reset", StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")
        StreamMessageV31 reset3 = new StreamMessageV31(new MessageID("subscriberId3", 0, 5145, 0, "publisherId", ""), null,
                StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE, StreamMessage.EncryptionType.RSA, "reset", StreamMessage.SignatureType.SIGNATURE_TYPE_ETH, "signature")

        when:
        util.handleGroupKeyRequest(request1) // should store subscriberId1 --> pk1
        util.handleGroupKeyRequest(request2) // should store subscriberId2 --> pk2
        util.handleGroupKeyRequest(request3) // should store subscriberId3 --> pk3
        util.rekey("streamId")
        then:
        1 * addressValidityUtil2.isValidSubscriber("streamId", "subscriberId1") >> true
        1 * addressValidityUtil2.isValidSubscriber("streamId", "subscriberId2") >> true
        1 * addressValidityUtil2.isValidSubscriber("streamId", "subscriberId3") >> true
        3 * storage.getKeysBetween("streamId", 123L, 456L) >> [genKey(32, new Date(123))]
        1 * addressValidityUtil2.getLocalSubscribersSet("streamId") >> ["subscriberId1", "subscriberId3"]
        2 * messageCreationUtil.createGroupKeyReset(*_) >> { arguments ->
            if (arguments[0] == "subscriberId1") {
                assert arguments[1] == "streamId"
                EncryptedGroupKey key = arguments[2]
                resetKey1 = key.getDecrypted(encryptionUtil1)
                return reset1
            } else {
                assert arguments[0] == "subscriberId3"
                assert arguments[1] == "streamId"
                EncryptedGroupKey key = arguments[2]
                resetKey3 = key.getDecrypted(encryptionUtil3)
                return reset3
            }
        }
        published.size() == 5
        resetKey1.groupKeyHex == resetKey3.groupKeyHex
        1 * storage.addKey(*_) >> { arguments ->
            assert arguments[0] == "streamId"
            UnencryptedGroupKey key = arguments[1]
            assert key.groupKeyHex == resetKey1.groupKeyHex
        }
        (published[3] == reset1 && published[4] == reset3) || (published[3] == reset3 && published[4] == reset1)
    }
}
