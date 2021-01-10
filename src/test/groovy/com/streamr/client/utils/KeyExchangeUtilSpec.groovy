package com.streamr.client.utils

import com.streamr.client.protocol.message_layer.GroupKeyAnnounce
import com.streamr.client.protocol.message_layer.GroupKeyRequest
import com.streamr.client.protocol.message_layer.GroupKeyResponse
import com.streamr.client.protocol.message_layer.MessageID
import com.streamr.client.protocol.message_layer.StreamMessage
import com.streamr.client.protocol.message_layer.StreamrSpecification
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer
import java.util.function.Function

class KeyExchangeUtilSpec extends StreamrSpecification {
    KeyExchangeUtil util
    GroupKeyStore keyStore
    MessageCreationUtil messageCreationUtil
    Consumer<StreamMessage> publish
    KeyExchangeUtil.OnNewKeysFunction onNewKeysFunction
    ArrayList<StreamMessage> published
    StreamMessage response = new StreamMessage.Builder()
            .withMessageId(new MessageID("subscriberId", 0, 5145, 0, publisherId, ""))
            .withMessageType(null)
            .withMessageType(StreamMessage.MessageType.GROUP_KEY_RESPONSE)
            .withSerializedContent("response")
            .withContentType(StreamMessage.ContentType.JSON)
            .withEncryptionType(StreamMessage.EncryptionType.RSA)
            .withGroupKeyId(null)
            .withNewGroupKey(null)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .withSignature("signature")
            .createStreamMessage()
    EncryptionUtil encryptionUtil = new EncryptionUtil()
    AddressValidityUtil addressValidityUtil = new AddressValidityUtil({ String id -> new ArrayList<>()}, { String s1, String s2 -> s1 == "streamId" && s2 == "subscriberId"},
            { String id -> new ArrayList<>()}, { String s, String p -> true})
    List<GroupKey> keysReportedToOnNewKeys

    void setup() {
        keyStore = Mock(GroupKeyStore)
        messageCreationUtil = Mock(MessageCreationUtil)
        published = new ArrayList<>()
        publish = new Consumer<StreamMessage>() {
            @Override
            void accept(StreamMessage streamMessage) {
                published.add(streamMessage)
            }
        }
        keysReportedToOnNewKeys = []
        onNewKeysFunction = new KeyExchangeUtil.OnNewKeysFunction() {
            @Override
            void apply(String streamId, Address publisherId, Collection<GroupKey> keys) {
                keysReportedToOnNewKeys.addAll(keys)
            }
        }
        util = new KeyExchangeUtil(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil, publish, onNewKeysFunction)
    }

    void "handleGroupKeyRequest() should send group key response for the requested keys"() {
        MessageID id = new MessageID("publisherInbox", 0, 414, 0, subscriberId, "msgChainId")
        GroupKey key1 = GroupKey.generate()
        GroupKey key2 = GroupKey.generate()

        // Need to use Double because Moshi parser converts all JSON numbers to double
        GroupKeyRequest request = new GroupKeyRequest("requestId", "streamId", encryptionUtil.publicKeyAsPemString, [key1.groupKeyId, key2.groupKeyId])
        StreamMessage streamMessage = request.toStreamMessageBuilder(id, null).createStreamMessage()

        when:
        util.handleGroupKeyRequest(streamMessage)

        then:
        1 * keyStore.get("streamId", key1.groupKeyId) >> key1
        1 * keyStore.get("streamId", key2.groupKeyId) >> key2
        1 * messageCreationUtil.createGroupKeyResponse(_, _, _) >> { Address subId, GroupKeyRequest req, List<GroupKey> keys ->
            assert subId == subscriberId
            assert req == request
            assert keys == [key1, key2]
            return response
        }
        published.size() == 1
        published.get(0) == response

        then:
        // Remember the public key of the subscriber
        util.getKnownPublicKeysByPublisher().get(subscriberId) == encryptionUtil.publicKeyAsPemString
    }

    void "handleGroupKeyResponse() should decrypt keys, add keys to keyStore, and call onNewKeys function"() {
        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, publisherId, "msgChainId")
        GroupKey key = GroupKey.generate()
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptWithPublicKey(key, encryptionUtil.publicKey)

        GroupKeyResponse response = new GroupKeyResponse("requestId", "streamId", [encryptedKey])
        StreamMessage streamMessage = response.toStreamMessageBuilder(id, null)
                .withEncryptionType(StreamMessage.EncryptionType.RSA)
                .createStreamMessage()

        when:
        util.handleGroupKeyResponse(streamMessage)

        then:
        1 * keyStore.add("streamId", key)
        keysReportedToOnNewKeys == [key]
    }

    void "handleGroupKeyAnnounce() should RSA decrypt keys, add them to keyStore, and call onNewKeys function"() {
        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, publisherId, "msgChainId")
        GroupKey key = GroupKey.generate()
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptWithPublicKey(key, encryptionUtil.publicKey)

        GroupKeyAnnounce announce = new GroupKeyAnnounce("streamId", [encryptedKey])
        StreamMessage streamMessage = announce.toStreamMessageBuilder(id, null)
                .withEncryptionType(StreamMessage.EncryptionType.RSA)
                .createStreamMessage()

        when:
        util.handleGroupKeyAnnounce(streamMessage)

        then:
        1 * keyStore.add("streamId", key)
        keysReportedToOnNewKeys == [key]
    }

    void "handleGroupKeyAnnounce() should AES decrypt keys, add them to keyStore, and call onNewKeys function"() {
        MessageID id = new MessageID("subscriberInbox", 0, 414, 0, publisherId, "msgChainId")
        GroupKey keyToEncrypt = GroupKey.generate()
        GroupKey keyToEncryptWith = GroupKey.generate()
        EncryptedGroupKey encryptedKey = EncryptionUtil.encryptGroupKey(keyToEncrypt, keyToEncryptWith)

        GroupKeyAnnounce announce = new GroupKeyAnnounce("streamId", [encryptedKey])
        StreamMessage streamMessage = announce.toStreamMessageBuilder(id, null)
                .withGroupKeyId(keyToEncryptWith.getGroupKeyId())
                .withEncryptionType(StreamMessage.EncryptionType.AES)
                .createStreamMessage()

        when:
        util.handleGroupKeyAnnounce(streamMessage)

        then:
        1 * keyStore.get(announce.getStreamId(), keyToEncryptWith.getGroupKeyId()) >> keyToEncryptWith
        1 * keyStore.add(announce.getStreamId(), keyToEncrypt)
        keysReportedToOnNewKeys == [keyToEncrypt]
    }

    void "keyRevocationNeeded() should not revoke if checked recently"() {
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
        util = new KeyExchangeUtil(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, onNewKeysFunction)

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

    void "keyRevocationNeeded() should not revoke if enough time elapsed but less than threshold"() {
        AddressValidityUtil addressValidityUtil2 = Mock(AddressValidityUtil)
        Clock clock = Mock(Clock)
        Instant now = Instant.now()
        clock.instant() >> now
        util = new KeyExchangeUtil(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, onNewKeysFunction, clock)

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
        util = new KeyExchangeUtil(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, onNewKeysFunction)
        when:
        boolean res = util.keyRevocationNeeded("streamId")
        then:
        1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >> KeyExchangeUtil.REVOCATION_THRESHOLD
        res
    }

    void "should rekey by sending group key announce messages to key exchange streams"() {
        AddressValidityUtil addressValidityUtil2 = Mock(AddressValidityUtil)
        util = new KeyExchangeUtil(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil2, publish, onNewKeysFunction)

        // Set some public keys for subscribers
        util.getKnownPublicKeysByPublisher().put(getSubscriberId(1), new EncryptionUtil().publicKeyAsPemString)
        util.getKnownPublicKeysByPublisher().put(getSubscriberId(2), new EncryptionUtil().publicKeyAsPemString)
        util.getKnownPublicKeysByPublisher().put(getSubscriberId(3), new EncryptionUtil().publicKeyAsPemString)

        def msgId = new MessageID(
                "keyexchange-sub1",
                0,
                0,
                0,
                publisherId,
                "msgChainId")
        StreamMessage announce1 = new GroupKeyAnnounce("streamId", [])
                .toStreamMessageBuilder(msgId, null)
                .createStreamMessage()
        StreamMessage announce3 = new GroupKeyAnnounce("streamId", [])
                .toStreamMessageBuilder(msgId, null)
                .createStreamMessage()

        when:
        util.rekey("streamId", true)

        then:
        // Should check current subscribers with AddressValidityUtil, which responds that subscribers 1 and 3 are still active
        1 * addressValidityUtil2.getSubscribersSet("streamId", true) >> [getSubscriberId(1), getSubscriberId(3)].toSet()
        // Add new key to keystore
        1 * keyStore.add("streamId", _)
        1 * messageCreationUtil.createGroupKeyAnnounce(getSubscriberId(1), "streamId", _, _) >> announce1
        0 * messageCreationUtil.createGroupKeyAnnounce(getSubscriberId(2), "streamId", _, _) // don't call for subscriber 2
        1 * messageCreationUtil.createGroupKeyAnnounce(getSubscriberId(3), "streamId", _, _) >> announce3

        published.size() == 2
        published[0] == announce1
        published[1] == announce3
    }
}
