package com.streamr.client.stream;

import com.streamr.client.crypto.RsaKeyPair;
import com.streamr.client.java.util.Objects;
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage;
import com.streamr.client.protocol.message_layer.EncryptedGroupKey;
import com.streamr.client.protocol.message_layer.GroupKeyAnnounce;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.GroupKeyResponse;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.AddressValidityUtil;
import com.streamr.ethereum.common.Address;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class for key exchange.
 *
 * <ul>
 *   <li>Publishes messages to key exchange streams (on rotate and rekey)
 *   <li>Handles messages published to key exchange streams (requests, responses, announces)
 *   <li>Manages the keys in the GroupKeyStore
 * </ul>
 */
public class KeyExchangeUtil {
  private static final Logger log = LoggerFactory.getLogger(KeyExchangeUtil.class);
  private final Clock clock;
  public static final int REVOCATION_THRESHOLD = 5;
  public static final int REVOCATION_DELAY = 10; // in minutes

  private final GroupKeyStore keyStore;
  private final MessageCreationUtil messageCreationUtil;
  private final RsaKeyPair rsaKeyPair;
  private final AddressValidityUtil addressValidityUtil;
  private final Consumer<StreamMessage> publishFunction;
  private final OnNewKeysFunction onNewKeysFunction;
  private Instant lastCallToCheckRevocation = Instant.MIN;
  private final Map<Address, String> publicKeys = new HashMap<>();

  public static final String KEY_EXCHANGE_STREAM_PREFIX = "SYSTEM/keyexchange/";

  public KeyExchangeUtil(
      GroupKeyStore keyStore,
      MessageCreationUtil messageCreationUtil,
      final RsaKeyPair rsaKeyPair,
      AddressValidityUtil addressValidityUtil,
      Consumer<StreamMessage> publishFunction,
      OnNewKeysFunction onNewKeysFunction,
      Clock clock) {
    this.keyStore = keyStore;
    this.messageCreationUtil = messageCreationUtil;
    this.rsaKeyPair = rsaKeyPair;
    this.addressValidityUtil = addressValidityUtil;
    this.publishFunction = publishFunction;
    this.onNewKeysFunction = onNewKeysFunction;
    this.clock = clock;
  }

  public void handleGroupKeyRequest(StreamMessage streamMessage) {
    GroupKeyRequest request =
        (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

    String streamId = request.getStreamId();
    Address sender = streamMessage.getPublisherId();

    log.debug(
        "Subscriber {} is querying group keys for stream {}: {}. Key storage content is {}",
        streamMessage.getPublisherId(),
        streamId,
        request.getGroupKeyIds(),
        keyStore);

    final List<GroupKey> foundKeys = new ArrayList<>();
    final List<GroupKey> notFoundKeys = new ArrayList<>();
    for (final String groupKeyId : request.getGroupKeyIds()) {
      final GroupKey key = keyStore.get(request.getStreamId(), groupKeyId);
      if (key != null) {
        foundKeys.add(key);
      } else {
        notFoundKeys.add(key);
      }
    }

    if (!notFoundKeys.isEmpty()) {
      log.warn(
          "The following keys requested by subscriber {} in stream {} were not found in key store: {}",
          streamMessage.getPublisherId(),
          streamId,
          notFoundKeys);
    }

    StreamMessage response = messageCreationUtil.createGroupKeyResponse(sender, request, foundKeys);

    // For re-keys, remember the public key for this subscriber
    publicKeys.put(sender, request.getRsaPublicKey());

    publishFunction.accept(response);
  }

  public void handleGroupKeyResponse(StreamMessage streamMessage) {
    GroupKeyResponse response =
        (GroupKeyResponse) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

    log.debug(
        "Received group key response from publisher {} for stream {}, keys {}",
        streamMessage.getPublisherId(),
        response.getStreamId(),
        response.getKeys());

    if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.RSA) {
      handleNewRSAEncryptedKeys(
          response.getKeys(), response.getStreamId(), streamMessage.getPublisherId());
    } else {
      throw new RuntimeException("Unexpected EncryptionType: " + streamMessage.getEncryptionType());
    }
  }

  public void handleGroupKeyAnnounce(StreamMessage streamMessage) {
    GroupKeyAnnounce announce =
        (GroupKeyAnnounce) AbstractGroupKeyMessage.fromStreamMessage(streamMessage);

    log.debug(
        "Received group key announce from publisher {} for stream {}, keys {}",
        streamMessage.getPublisherId(),
        announce.getStreamId(),
        announce.getKeys());

    if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.RSA) {
      handleNewRSAEncryptedKeys(
          announce.getKeys(), announce.getStreamId(), streamMessage.getPublisherId());
    } else if (streamMessage.getEncryptionType() == StreamMessage.EncryptionType.AES) {
      handleNewAESEncryptedKeys(
          announce.getKeys(),
          announce.getStreamId(),
          streamMessage.getPublisherId(),
          streamMessage.getGroupKeyId());
    } else {
      throw new RuntimeException("Unexpected EncryptionType: " + streamMessage.getEncryptionType());
    }
  }

  private void handleNewRSAEncryptedKeys(
      Collection<EncryptedGroupKey> encryptedKeys, String streamId, Address publisherId) {
    List<GroupKey> keys =
        encryptedKeys.stream()
            .map(
                encryptedKey -> {
                  try {
                    return EncryptionUtil.decryptWithPrivateKey(
                        this.rsaKeyPair.getRsaPrivateKey(), encryptedKey);
                  } catch (Exception e) {
                    log.error(
                        "Unable to decrypt group key {} for stream {}",
                        encryptedKey.getGroupKeyId(),
                        streamId,
                        e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    handleNewKeys(streamId, publisherId, keys);
  }

  public void handleNewAESEncryptedKeys(
      List<EncryptedGroupKey> encryptedKeys,
      String streamId,
      Address publisherId,
      String groupKeyId) {
    List<GroupKey> keys =
        encryptedKeys.stream()
            .map(
                encryptedKey -> {
                  try {
                    GroupKey keyToDecryptWith = keyStore.get(streamId, groupKeyId);
                    if (keyToDecryptWith == null) {
                      throw new Exception(
                          String.format(
                              "Key %s for stream %s was not found in keyStore",
                              groupKeyId, streamId));
                    }
                    return EncryptionUtil.decryptGroupKey(encryptedKey, keyToDecryptWith);
                  } catch (Exception e) {
                    log.error(
                        "Unable to decrypt group key {} for stream {}",
                        encryptedKey.getGroupKeyId(),
                        streamId,
                        e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    handleNewKeys(streamId, publisherId, keys);
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

  public GroupKey rekey(String streamId, boolean getSubscribersLocally) {
    GroupKey newKey = GroupKey.generate();
    keyStore.add(streamId, newKey);

    Set<Address> trueSubscribersSet =
        addressValidityUtil.getSubscribersSet(streamId, getSubscribersLocally);
    Set<Address> revoked = new HashSet<>();

    for (Address subscriberId :
        publicKeys.keySet()) { // iterating over local cache of Ethereum address --> RSA public key
      if (trueSubscribersSet.contains(
          subscriberId)) { // if still valid subscriber, send the new key
        String publicKey = publicKeys.get(subscriberId);
        StreamMessage announce =
            messageCreationUtil.createGroupKeyAnnounce(
                subscriberId, streamId, publicKey, Collections.singletonList(newKey));
        publishFunction.accept(announce);
      } else { // no longer a valid subscriber, to be removed from local cache
        revoked.add(subscriberId);
      }
    }
    revoked.forEach(
        publicKeys
            ::remove); // remove all revoked (Ethereum address --> RSA public key) from local cache
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

  public Map<Address, String> getKnownPublicKeysByPublisher() {
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
