package com.streamr.client.utils;

import com.streamr.client.exceptions.KeyAlreadyExistsException;
import com.streamr.client.protocol.message_layer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This is a helper class for key exchange.
 *
 * - Publishes messages to key exchange streams (on rotate and rekey)
 * - Handles messages published to key exchange streams (requests, responses, announces)
 * - Manages the keys in the GroupKeyStore
 */
public class KeyExchangeUtil {
    private static final Logger log = LoggerFactory.getLogger(KeyExchangeUtil.class);
    private final Clock clock;
    public static final int REVOCATION_THRESHOLD = 5;
    public static final int REVOCATION_DELAY = 10; // in minutes

    private final GroupKeyStore keyStore;
    private final MessageCreationUtil messageCreationUtil;
    private final EncryptionUtil encryptionUtil;
    private final AddressValidityUtil addressValidityUtil;
    private final Consumer<StreamMessage> publishFunction;
    private final OnNewKeysFunction onNewKeysFunction;
    private Instant lastCallToCheckRevocation = Instant.MIN;
    private final HashMap<Address, String> publicKeys = new HashMap<>();

    public static final String KEY_EXCHANGE_STREAM_PREFIX = "SYSTEM/keyexchange/";

    public KeyExchangeUtil(GroupKeyStore keyStore, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Consumer<StreamMessage> publishFunction,
                           OnNewKeysFunction onNewKeysFunction) {
        this(keyStore, messageCreationUtil, encryptionUtil, addressValidityUtil, publishFunction,
                onNewKeysFunction, Clock.systemDefaultZone());
    }

    // constructor used for testing in KeyExchangeUtilSpec
    public KeyExchangeUtil(GroupKeyStore keyStore, MessageCreationUtil messageCreationUtil, EncryptionUtil encryptionUtil,
                           AddressValidityUtil addressValidityUtil, Consumer<StreamMessage> publishFunction,
                           OnNewKeysFunction onNewKeysFunction, Clock clock) {
        this.keyStore = keyStore;
        this.messageCreationUtil = messageCreationUtil;
        this.encryptionUtil = encryptionUtil;
        this.addressValidityUtil = addressValidityUtil;
        this.publishFunction = publishFunction;
        this.onNewKeysFunction = onNewKeysFunction;
        this.clock = clock;
    }

    public void handleGroupKeyRequest(StreamMessage streamMessage) {
        GroupKeyRequest request = (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

        String streamId = request.getStreamId();
        Address sender = streamMessage.getPublisherId();

        log.debug("Subscriber {} is querying group keys for stream {}: {}. Key storage content is {}",
                streamMessage.getPublisherId(), streamId, request.getGroupKeyIds(), keyStore);

        ArrayList<GroupKey> foundKeys = new ArrayList<>();
        ArrayList<GroupKey> notFoundKeys = new ArrayList<>();
        for (String groupKeyId : request.getGroupKeyIds()) {
            GroupKey key = keyStore.get(request.getStreamId(), groupKeyId);
            if (key != null) {
                foundKeys.add(key);
            } else {
                notFoundKeys.add(key);
            }
        }

        if (!notFoundKeys.isEmpty()) {
            log.warn("The following keys requested by subscriber {} in stream {} were not found in key store: {}",
                    streamMessage.getPublisherId(), streamId, notFoundKeys);
        }

        StreamMessage response = messageCreationUtil.createGroupKeyResponse(sender, request, foundKeys);

        // For re-keys, remember the public key for this subscriber
        publicKeys.put(sender, request.getPublicKey());

        publishFunction.accept(response);
    }

    public void handleGroupKeyResponse(StreamMessage streamMessage) {
        GroupKeyResponse response = (GroupKeyResponse) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

        log.debug("Received group key response from publisher {} for stream {}, keys {}",
                streamMessage.getPublisherId(), response.getStreamId(), response.getKeys());

        List<GroupKey> keys;
        if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.RSA) {
            keys = decryptGroupKeysRSA(response.getKeys(), response);
        } else {
            throw new RuntimeException("Unexpected EncryptionType: " + streamMessage.getEncryptionType());
        }

        handleNewKeys(response.getStreamId(), streamMessage.getPublisherId(), keys);
    }

    private List<GroupKey> decryptGroupKeysRSA(List<EncryptedGroupKey> encryptedKeys, AbstractGroupKeyMessage groupKeyMessage) {
        return encryptedKeys.stream()
                .map(encryptedKey -> {
                    try {
                        return encryptionUtil.decryptWithPrivateKey(encryptedKey);
                    } catch (Exception e) {
                        log.error("Unable to decrypt group key {} for stream {}", encryptedKey.getGroupKeyId(), groupKeyMessage.getStreamId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<GroupKey> decryptGroupKeysAES(List<EncryptedGroupKey> encryptedKeys, AbstractGroupKeyMessage groupKeyMessage, String groupKeyId) {
        return encryptedKeys.stream()
                .map(encryptedKey -> {
                    try {
                        GroupKey keyToDecryptWith = keyStore.get(groupKeyMessage.getStreamId(), groupKeyId);
                        if (keyToDecryptWith == null) {
                            throw new Exception(String.format("Key %s for stream %s was not found in keyStore", groupKeyId, groupKeyMessage.getStreamId()));
                        }
                        return EncryptionUtil.decryptGroupKey(encryptedKey, keyToDecryptWith);
                    } catch (Exception e) {
                        log.error("Unable to decrypt group key {} for stream {}", encryptedKey.getGroupKeyId(), groupKeyMessage.getStreamId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void handleGroupKeyAnnounce(StreamMessage streamMessage) {
        GroupKeyAnnounce announce = (GroupKeyAnnounce) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

        log.debug("Received group key announce from publisher {} for stream {}, keys {}",
                streamMessage.getPublisherId(), announce.getStreamId(), announce.getKeys());

        List<GroupKey> keys;
        if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.RSA) {
            keys = decryptGroupKeysRSA(announce.getKeys(), announce);
        } else if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.AES) {
            keys = decryptGroupKeysAES(announce.getKeys(), announce, streamMessage.getGroupKeyId());
        } else {
            throw new RuntimeException("Unexpected EncryptionType: " + streamMessage.getEncryptionType());
        }

        handleNewKeys(announce.getStreamId(), streamMessage.getPublisherId(), keys);
    }

    public boolean keyRevocationNeeded(String streamId) {
        Instant now = clock.instant();
        boolean res = false;
        if (lastCallToCheckRevocation.plus(Duration.ofMinutes(REVOCATION_DELAY)).isBefore(now)) {
            res = addressValidityUtil.nbSubscribersToRevoke(streamId) >= REVOCATION_THRESHOLD;
        }
        lastCallToCheckRevocation = now;
        return res;
    }

    /**
     * Rotates the key by publishing a new GroupKeyAnnounce message on the stream,
     * encrypted with the key currently in use.
     */
    public void rotate(String streamId, GroupKey newKey, Date timestamp) {
        GroupKey currentKey = keyStore.getCurrentKey(streamId);
        if (currentKey == null) {
            throw new IllegalStateException("Can't rotate: there is no current key for stream " + streamId);
        }

        StreamMessage groupKeyResetMsg = messageCreationUtil.createGroupKeyAnnounceOnStream(streamId, Collections.singletonList(newKey), keyStore.getCurrentKey(streamId), timestamp);
        keyStore.add(streamId, newKey);
        publishFunction.accept(groupKeyResetMsg);
    }

    public GroupKey rekey(String streamId, boolean getSubscribersLocally) {
        GroupKey newKey = GroupKey.generate();
        keyStore.add(streamId, newKey);

        Set<Address> trueSubscribersSet = addressValidityUtil.getSubscribersSet(streamId, getSubscribersLocally);
        Set<Address> revoked = new HashSet<>();

        for (Address subscriberId: publicKeys.keySet() ) { // iterating over local cache of Ethereum address --> RSA public key
            if (trueSubscribersSet.contains(subscriberId)) { // if still valid subscriber, send the new key
                String publicKey = publicKeys.get(subscriberId);
                StreamMessage announce = messageCreationUtil.createGroupKeyAnnounceForSubscriber(subscriberId, streamId, publicKey, Collections.singletonList(newKey));
                publishFunction.accept(announce);
            } else { // no longer a valid subscriber, to be removed from local cache
                revoked.add(subscriberId);
            }
        }
        revoked.forEach(publicKeys::remove); // remove all revoked (Ethereum address --> RSA public key) from local cache
        return newKey;
    }

    public static String getKeyExchangeStreamId(Address recipientAddress) {
        return KEY_EXCHANGE_STREAM_PREFIX + recipientAddress;
    }

    public static boolean isKeyExchangeStreamId(String streamId) {
        return streamId.startsWith(KEY_EXCHANGE_STREAM_PREFIX);
    }

    public static Address getRecipientFromKeyExchangeStreamId(String keyExchangeStreamId) {
        return new Address(keyExchangeStreamId.substring(KEY_EXCHANGE_STREAM_PREFIX.length()));
    }

    public HashMap<Address, String> getKnownPublicKeysByPublisher() {
        return publicKeys;
    }

    @FunctionalInterface
    public interface OnNewKeysFunction {
        void apply(String streamId, Address publisherId, Collection<GroupKey> keys);
    }

    private void handleNewKeys(String streamId, Address publisherId, List<GroupKey> newKeys) {
        for (GroupKey key : newKeys) {
            try {
                keyStore.add(streamId, key);
            } catch (KeyAlreadyExistsException e) {
                log.warn("Key {} already exists in key store, skipping", key.getGroupKeyId());
            }
        }
        onNewKeysFunction.apply(streamId, publisherId, newKeys);
    }
}
