package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.message_layer.GroupKeyAnnounce;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.GroupKeyResponse;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.testing.TestingAddressesX;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyExchangeUtilTest {
  public static final Address SUBSCRIBER_ID =
      new Address("0x5555555555555555555555555555555555555555");
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");

  public static Address createSubscriberId(final int number) {
    if (number > 15) {
      throw new AssertionError("subscriberId number must be less than 16");
    }
    return new Address("0x555555555555555555555555555555555555555" + Integer.toHexString(number));
  }

  private KeyExchangeUtil util;
  private GroupKeyStore keyStore;
  private MessageCreationUtil messageCreationUtil;
  private Consumer<StreamMessage> publish;
  private KeyExchangeUtil.OnNewKeysFunction onNewKeysFunction;
  private ArrayList<StreamMessage> published;
  private MessageId messageId =
      new MessageId.Builder()
          .withStreamId("subscriberId")
          .withStreamPartition(0)
          .withTimestamp(5145)
          .withSequenceNumber(0)
          .withPublisherId(PUBLISHER_ID)
          .withMsgChainId("")
          .createMessageId();
  private final byte[] payload = "response".getBytes(StandardCharsets.UTF_8);
  private final StreamMessage.Content content =
      StreamMessage.Content.Factory.withJsonAsPayload(payload);
  private StreamMessage response =
      new StreamMessage.Builder()
          .withMessageId(messageId)
          .withMessageType(null)
          .withMessageType(StreamMessage.MessageType.GROUP_KEY_RESPONSE)
          .withContent(content)
          .withEncryptionType(StreamMessage.EncryptionType.RSA)
          .withGroupKeyId(null)
          .withNewGroupKey(null)
          .withSignatureType(StreamMessage.SignatureType.ETH)
          .withSignature("signature")
          .createStreamMessage();
  private EncryptionUtil encryptionUtil = new EncryptionUtil();
  private AddressValidityUtil addressValidityUtil =
      new AddressValidityUtil(
          (String id) -> {
            return new ArrayList<Address>();
          },
          (String s1, Address s2) -> {
            return s1.equals("streamId") && s2.equals(new Address("subscriberId"));
          },
          (String id) -> {
            return new ArrayList<Address>();
          },
          (String s, Address p) -> {
            return true;
          });
  private List<GroupKey> keysReportedToOnNewKeys;

  @BeforeEach
  void setup() {
    messageCreationUtil = new MessageCreationUtil(null, null);
    published = new ArrayList<>();
    publish =
        new Consumer<StreamMessage>() {
          @Override
          public void accept(StreamMessage streamMessage) {
            published.add(streamMessage);
          }
        };
    keysReportedToOnNewKeys = new ArrayList<>();
    onNewKeysFunction =
        new KeyExchangeUtil.OnNewKeysFunction() {
          @Override
          public void apply(String streamId, Address publisherId, Collection<GroupKey> keys) {
            keysReportedToOnNewKeys.addAll(keys);
          }
        };
    keyStore =
        new GroupKeyStore() {
          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return null;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
  }

  @Test
  void handleGroupKeyRequestShouldSendGroupKeyResponseForRequestedKeys() {
    MessageId id =
        new MessageId.Builder()
            .withStreamId("publisherInbox")
            .withStreamPartition(0)
            .withTimestamp(414)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddressesX.SUBSCRIBER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    GroupKey key1 = GroupKey.generate();
    GroupKey key2 = GroupKey.generate();

    // Need to use Double because Moshi parser converts all JSON numbers to double
    List<String> list = new ArrayList<>();
    list.add(key1.getGroupKeyId());
    list.add(key2.getGroupKeyId());
    GroupKeyRequest request =
        new GroupKeyRequest(
            "requestId", "streamId", encryptionUtil.getPublicKeyAsPemString(), list);
    keyStore =
        new GroupKeyStore() {
          private int getCallCount = 0;

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            if (getCallCount > 2) {
              throw new RuntimeException("GroupKey.get(...) called more that twice!");
            }
            getCallCount++;
            switch (getCallCount) {
              case 1:
                return key1;
              case 2:
                return key2;
            }
            throw new RuntimeException("Unexpected mock state!");
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {}
        };
    messageCreationUtil =
        new MessageCreationUtil(null, null) {
          @Override
          public StreamMessage createGroupKeyResponse(
              final Address subscriberAddress,
              final GroupKeyRequest req,
              final List<GroupKey> groupKeys) {
            assertEquals(TestingAddressesX.SUBSCRIBER_ID, subscriberAddress);
            assertEquals(request, req);
            List<GroupKey> expectedGroupKeys = new ArrayList<>();
            expectedGroupKeys.add(key1);
            expectedGroupKeys.add(key2);
            assertEquals(expectedGroupKeys, groupKeys);
            return response;
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
    StreamMessage streamMessage = request.toStreamMessageBuilder(id, null).createStreamMessage();
    util.handleGroupKeyRequest(streamMessage);

    assertEquals(1, published.size());
    assertEquals(response, published.get(0));
    // Remember the public key of the subscriber
    assertEquals(
        encryptionUtil.getPublicKeyAsPemString(),
        util.getKnownPublicKeysByPublisher().get(TestingAddressesX.SUBSCRIBER_ID));
  }

  @Test
  void handleGroupKeyResponseShouldDecryptKeysAddKeysToKeyStoreAndCallOnNewKeysFunction() {
    MessageId id =
        new MessageId.Builder()
            .withStreamId("subscriberInbox")
            .withStreamPartition(0)
            .withTimestamp(414)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddressesX.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    GroupKey key = GroupKey.generate();
    EncryptedGroupKey encryptedKey =
        EncryptionUtil.encryptWithPublicKey(key, encryptionUtil.getPublicKey());
    List<EncryptedGroupKey> encryptedKeys = new ArrayList<>();
    encryptedKeys.add(encryptedKey);
    GroupKeyResponse response = new GroupKeyResponse("requestId", "streamId", encryptedKeys);
    final int[] storeKeyCallCount = {0};
    keyStore =
        new GroupKeyStore() {

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return null;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {
            storeKeyCallCount[0]++;
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
    StreamMessage streamMessage =
        response
            .toStreamMessageBuilder(id, null)
            .withEncryptionType(StreamMessage.EncryptionType.RSA)
            .createStreamMessage();

    util.handleGroupKeyResponse(streamMessage);

    // 1 * keyStore.add("streamId", key)
    assertEquals(1, storeKeyCallCount[0]);
    List<GroupKey> expectedKeys = new ArrayList<>();
    expectedKeys.add(key);
    assertEquals(expectedKeys, keysReportedToOnNewKeys);
  }

  @Test
  void handleGroupKeyAnnounceShouldRsaDecryptKeysAddThemToKeyStoreAndCallOnNewKeysFunction() {
    MessageId id =
        new MessageId.Builder()
            .withStreamId("subscriberInbox")
            .withStreamPartition(0)
            .withTimestamp(414)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddressesX.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    GroupKey key = GroupKey.generate();
    EncryptedGroupKey encryptedKey =
        EncryptionUtil.encryptWithPublicKey(key, encryptionUtil.getPublicKey());
    List<EncryptedGroupKey> encryptedKeys = new ArrayList<>();
    encryptedKeys.add(encryptedKey);
    GroupKeyAnnounce announce = new GroupKeyAnnounce("streamId", encryptedKeys);
    StreamMessage streamMessage =
        announce
            .toStreamMessageBuilder(id, null)
            .withEncryptionType(StreamMessage.EncryptionType.RSA)
            .createStreamMessage();
    final int[] storeKeyCallCount = {0};
    keyStore =
        new GroupKeyStore() {

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return null;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {
            storeKeyCallCount[0]++;
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
    util.handleGroupKeyAnnounce(streamMessage);

    // 1 * keyStore.add("streamId", key)
    assertEquals(1, storeKeyCallCount[0]);
    List<GroupKey> expectedKeys = new ArrayList<>();
    expectedKeys.add(key);
    assertEquals(expectedKeys, keysReportedToOnNewKeys);
  }

  @Test
  void handleGroupKeyAnnounceShouldAesDecryptKeysAddThemToKeyStoreAndCallOnNewKeysFunction() {
    MessageId id =
        new MessageId.Builder()
            .withStreamId("subscriberInbox")
            .withStreamPartition(0)
            .withTimestamp(414)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddressesX.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    GroupKey keyToEncrypt = GroupKey.generate();
    GroupKey keyToEncryptWith = GroupKey.generate();
    EncryptedGroupKey encryptedKey = EncryptionUtil.encryptGroupKey(keyToEncrypt, keyToEncryptWith);
    List<EncryptedGroupKey> encryptedKeys = new ArrayList<>();
    encryptedKeys.add(encryptedKey);
    GroupKeyAnnounce announce = new GroupKeyAnnounce("streamId", encryptedKeys);
    StreamMessage streamMessage =
        announce
            .toStreamMessageBuilder(id, null)
            .withGroupKeyId(keyToEncryptWith.getGroupKeyId())
            .withEncryptionType(StreamMessage.EncryptionType.AES)
            .createStreamMessage();
    final int[] storeKeyCallCount = {0};
    keyStore =
        new GroupKeyStore() {

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return keyToEncryptWith;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {
            storeKeyCallCount[0]++;
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
    util.handleGroupKeyAnnounce(streamMessage);

    // 1 * keyStore.get(announce.getStreamId(), keyToEncryptWith.getGroupKeyId()) >>
    // keyToEncryptWith
    // 1 * keyStore.add(announce.getStreamId(), keyToEncrypt)
    assertEquals(1, storeKeyCallCount[0]);
    List<GroupKey> expectedKeys = new ArrayList<>();
    expectedKeys.add(keyToEncrypt);
    assertEquals(expectedKeys, keysReportedToOnNewKeys);
  }

  @Test
  void keyRevocationNeededShouldNotRevokeIfCheckedRecently() {
    final int[] callCount = {0};
    AddressValidityUtil addressValidityUtil2 =
        new AddressValidityUtil(
            new Function<String, List<Address>>() {
              @Override
              public List<Address> apply(String s) {
                if (s == "streamId") {
                  callCount[0]++;
                }
                return new ArrayList<>();
              }
            },
            null,
            null,
            null);
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil2,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());

    boolean res = util.keyRevocationNeeded("streamId");
    assertEquals(1, callCount[0]);
    assertTrue(!res);
    res = util.keyRevocationNeeded("streamId");
    assertEquals(1, callCount[0]); // not enough time elapsed since last call
    assertTrue(!res);
  }

  @Test
  void keyRevocationNeededShouldNotRevokeIfEnoughTimeElapsedButLessThanThreshold() {
    AddressValidityUtil addressValidityUtil2 =
        new AddressValidityUtil(null, null, null, null) {
          private int callCount = 0;

          @Override
          public int nbSubscribersToRevoke(final String streamId) {
            callCount++;
            switch (callCount) {
              case 1:
                return 0;
              case 2:
                return KeyExchangeUtil.REVOCATION_THRESHOLD - 1;
            }
            throw new RuntimeException("Unexpected mock state!");
          }
        };
    Instant now = Instant.now();
    Clock clock =
        new Clock() {
          private int instantCallCount = 0;

          @Override
          public ZoneId getZone() {
            return null;
          }

          @Override
          public Clock withZone(final ZoneId zone) {
            return null;
          }

          @Override
          public Instant instant() {
            instantCallCount++;
            switch (instantCallCount) {
              case 1:
                return now;
              case 2:
                return now.plus(Duration.ofMinutes(KeyExchangeUtil.REVOCATION_DELAY + 1));
            }
            throw new RuntimeException("Unexpected mock state!");
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil2,
            publish,
            onNewKeysFunction,
            clock);

    boolean res = util.keyRevocationNeeded("streamId");
    // 1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >> 0
    assertTrue(!res);

    res = util.keyRevocationNeeded("streamId");
    // 1 * clock.instant() >> now + Duration.ofMinutes(KeyExchangeUtil.REVOCATION_DELAY + 1)
    // 1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >>
    // KeyExchangeUtil.REVOCATION_THRESHOLD - 1
    assertTrue(!res);
  }

  @Test
  void shouldRevokeIfThresholdReached() {
    final int[] nbSubscribersToRevokeCallCount = new int[1];
    AddressValidityUtil addressValidityUtil2 =
        new AddressValidityUtil(null, null, null, null) {
          @Override
          public int nbSubscribersToRevoke(final String streamId) {
            nbSubscribersToRevokeCallCount[0]++;
            return KeyExchangeUtil.REVOCATION_THRESHOLD;
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil2,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());
    boolean res = util.keyRevocationNeeded("streamId");
    // 1 * addressValidityUtil2.nbSubscribersToRevoke("streamId") >>
    // KeyExchangeUtil.REVOCATION_THRESHOLD
    assertEquals(1, nbSubscribersToRevokeCallCount[0]);
    assertTrue(res);
  }

  @Test
  void shouldRekeyBySendingGroupKeyAnnounceNessagesToKeyExchangeStreams() {
    final int[] getSubscribersSetCallCount = {0};
    AddressValidityUtil addressValidityUtil2 =
        new AddressValidityUtil(null, null, null, null) {
          @Override
          public Set<Address> getSubscribersSet(final String streamId, final boolean locally) {
            getSubscribersSetCallCount[0]++;
            Set<Address> set = new HashSet<>();
            set.add(TestingAddressesX.createSubscriberId(1));
            set.add(TestingAddressesX.createSubscriberId(3));
            return set;
          }
        };
    final int[] storeKeyCallCount = {0};
    keyStore =
        new GroupKeyStore() {

          @Override
          public boolean contains(final String groupKeyId) {
            return false;
          }

          @Override
          public GroupKey get(final String streamId, final String groupKeyId) {
            return null;
          }

          @Override
          protected void storeKey(final String streamId, final GroupKey key) {
            storeKeyCallCount[0]++;
          }
        };
    MessageId msgId =
        new MessageId.Builder()
            .withStreamId("keyexchange-sub1")
            .withStreamPartition(0)
            .withTimestamp(0)
            .withSequenceNumber(0)
            .withPublisherId(TestingAddressesX.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage announce1 =
        new GroupKeyAnnounce("streamId", new ArrayList<>())
            .toStreamMessageBuilder(msgId, null)
            .createStreamMessage();
    StreamMessage announce3 =
        new GroupKeyAnnounce("streamId", new ArrayList<>())
            .toStreamMessageBuilder(msgId, null)
            .createStreamMessage();
    final int[] createGroupKeyResponseCallCount = {0};
    messageCreationUtil =
        new MessageCreationUtil(null, null) {

          @Override
          public StreamMessage createGroupKeyAnnounce(
              final Address subscriberAddress,
              final String streamId,
              final String publicKey,
              final List<GroupKey> groupKeys) {
            createGroupKeyResponseCallCount[0]++;
            switch (createGroupKeyResponseCallCount[0]) {
              case 1:
                return announce1;
              case 2:
                return announce3;
            }
            throw new RuntimeException("Unexpected mock state!");
          }
        };
    util =
        new KeyExchangeUtil(
            keyStore,
            messageCreationUtil,
            encryptionUtil,
            addressValidityUtil2,
            publish,
            onNewKeysFunction,
            Clock.systemDefaultZone());

    // Set some public keys for subscribers
    util.getKnownPublicKeysByPublisher()
        .put(
            TestingAddressesX.createSubscriberId(1),
            new EncryptionUtil().getPublicKeyAsPemString());
    util.getKnownPublicKeysByPublisher()
        .put(
            TestingAddressesX.createSubscriberId(2),
            new EncryptionUtil().getPublicKeyAsPemString());
    util.getKnownPublicKeysByPublisher()
        .put(
            TestingAddressesX.createSubscriberId(3),
            new EncryptionUtil().getPublicKeyAsPemString());

    util.rekey("streamId", true);

    // Should check current subscribers with AddressValidityUtil, which responds that subscribers 1
    // and 3 are still active
    // 1 * addressValidityUtil2.getSubscribersSet("streamId", true) >>
    // [TestingAddresses.createSubscriberId(1), TestingAddresses.createSubscriberId(3)].toSet()
    assertEquals(1, getSubscribersSetCallCount[0]);
    // Add new key to keystore
    // 1 * keyStore.add("streamId", _)
    assertEquals(1, storeKeyCallCount[0]);
    // 1 * messageCreationUtil.createGroupKeyAnnounce(TestingAddresses.createSubscriberId(1),
    // "streamId", _, _) >> announce1

    // 0 * messageCreationUtil.createGroupKeyAnnounce(TestingAddresses.createSubscriberId(2),
    // "streamId", _, _) // don't call for subscriber 2

    // 1 * messageCreationUtil.createGroupKeyAnnounce(TestingAddresses.createSubscriberId(3),
    // "streamId", _, _) >> announce3
    assertEquals(2, createGroupKeyResponseCallCount[0]);
    assertEquals(2, published.size());
    assertEquals(announce1, published.get(0));
    assertEquals(announce3, published.get(1));
  }
}
