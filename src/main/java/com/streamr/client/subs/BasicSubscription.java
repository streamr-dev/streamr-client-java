package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.common.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.DecryptionQueues;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.GroupKeyStore;
import com.streamr.client.utils.KeyExchangeUtil;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.OrderingUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public abstract class BasicSubscription extends Subscription {
  public static final int MAX_NB_GROUP_KEY_REQUESTS = 10;

  protected OrderingUtil orderingUtil;
  private final ConcurrentMap<String, Timer> pendingGroupKeyRequests = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Integer> nbGroupKeyRequestsCalls = new ConcurrentHashMap<>();
  private final HashSet<String> alreadyFailedToDecrypt = new HashSet<>();

  protected final DecryptionQueues decryptionQueues;
  private final GroupKeyRequestFunction groupKeyRequestFunction;

  public BasicSubscription(
      String streamId,
      int partition,
      MessageHandler handler,
      GroupKeyStore keyStore,
      KeyExchangeUtil keyExchangeUtil,
      GroupKeyRequestFunction groupKeyRequestFunction,
      long propagationTimeout,
      long resendTimeout,
      boolean skipGapsOnFullQueue) {
    super(
        streamId,
        partition,
        handler,
        keyStore,
        keyExchangeUtil,
        propagationTimeout,
        resendTimeout,
        skipGapsOnFullQueue);

    orderingUtil =
        new OrderingUtil(
            streamId,
            partition,
            this::handleInOrder,
            (MessageRef from, MessageRef to, Address publisherId, String msgChainId) -> {
              throw new GapDetectedException(
                  streamId, partition, from, to, publisherId, msgChainId);
            },
            this.propagationTimeout,
            this.resendTimeout,
            this.skipGapsOnFullQueue);

    decryptionQueues = new DecryptionQueues(streamId, partition);

    this.groupKeyRequestFunction =
        groupKeyRequestFunction != null
            ? groupKeyRequestFunction
            : ((publisherId, groupKeyIds) ->
                getLogger()
                    .warn(
                        "Group key missing for stream "
                            + streamId
                            + " and publisher "
                            + publisherId
                            + " but no handler is set."));
  }

  @Override
  public void handleResentMessage(StreamMessage msg)
      throws GapDetectedException, UnsupportedMessageException {
    orderingUtil.add(msg);
  }

  @Override
  public void clear() {
    orderingUtil.clearGaps();
  }

  public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
    orderingUtil =
        new OrderingUtil(
            streamId,
            partition,
            this::handleInOrder,
            gapHandler,
            propagationTimeout,
            resendTimeout,
            skipGapsOnFullQueue);
  }

  public OrderedMsgChain.GapHandlerFunction getGapHandler() {
    return orderingUtil.getGapHandler();
  }

  protected void requestGroupKeyAndQueueMessage(StreamMessage msgToQueue) {
    Timer t =
        new Timer(
            String.format(
                "GroupKeyTimer-%s-%s",
                msgToQueue.getStreamId(), msgToQueue.getMessageRef().toString()),
            true);
    String groupKeyId = msgToQueue.getGroupKeyId();
    nbGroupKeyRequestsCalls.put(groupKeyId, 0);

    TimerTask request =
        new TimerTask() {
          @Override
          public void run() {
            synchronized (BasicSubscription.this) {
              if (pendingGroupKeyRequests.containsKey(groupKeyId)) {
                if (nbGroupKeyRequestsCalls.get(groupKeyId) < MAX_NB_GROUP_KEY_REQUESTS) {
                  nbGroupKeyRequestsCalls.put(
                      groupKeyId, nbGroupKeyRequestsCalls.get(groupKeyId) + 1);
                  groupKeyRequestFunction.apply(
                      msgToQueue.getPublisherId(),
                      Collections.singletonList(msgToQueue.getGroupKeyId()));
                  getLogger()
                      .info(
                          "Sent key request for stream {} publisher {}, key id {}",
                          streamId,
                          msgToQueue.getPublisherId(),
                          groupKeyId);
                } else {
                  getLogger()
                      .warn(
                          "Failed to receive group key {} from publisher {} after {} tries.",
                          groupKeyId,
                          msgToQueue.getPublisherId(),
                          MAX_NB_GROUP_KEY_REQUESTS);
                  cancelGroupKeyRequest(groupKeyId);
                }
              }
            }
          }
        };

    pendingGroupKeyRequests.put(groupKeyId, t);
    decryptionQueues.add(msgToQueue);
    t.schedule(request, 0, propagationTimeout);
  }

  private synchronized void cancelGroupKeyRequest(String groupKeyId) {
    if (pendingGroupKeyRequests.containsKey(groupKeyId)) {
      getLogger().trace("Pending group key request canceled for group key {}", groupKeyId);
      Timer timer = pendingGroupKeyRequests.get(groupKeyId);
      timer.cancel();
      timer.purge();
      pendingGroupKeyRequests.remove(groupKeyId);
    }
  }

  public ArrayList<OrderedMsgChain> getChains() {
    return orderingUtil.getChains();
  }

  private final class DecryptResult {
    private final boolean status;
    private final StreamMessage message;

    DecryptResult(final boolean status, final StreamMessage message) {
      this.status = status;
      this.message = message;
    }
  }

  private DecryptResult tryDecrypt(StreamMessage msg) throws UnableToDecryptException {
    // Key exchange messages are handled in a special way in KeyExchangeUtil
    if (msg.getMessageType() != StreamMessage.MessageType.STREAM_MESSAGE) {
      return new DecryptResult(true, msg);
    }

    // Nothing needs to be done here if the message is not encrypted
    if (msg.getEncryptionType() == StreamMessage.EncryptionType.NONE) {
      return new DecryptResult(true, msg);
    }

    try {
      GroupKey groupKey = keyStore.get(msg.getStreamId(), msg.getGroupKeyId());
      if (groupKey == null) {
        throw UnableToDecryptException.create(msg.getSerializedContent());
      }

      msg = EncryptionUtil.decryptStreamMessage(msg, groupKey);
      alreadyFailedToDecrypt.remove(msg.getGroupKeyId());
      return new DecryptResult(true, msg);
    } catch (UnableToDecryptException e) {
      if (alreadyFailedToDecrypt.contains(msg.getGroupKeyId())) {
        // even after receiving the latest group key, we still cannot decrypt
        throw e;
      } else {
        // Fail next time we come here
        alreadyFailedToDecrypt.add(msg.getGroupKeyId());
      }
      return new DecryptResult(false, msg);
    }
  }

  private void handleInOrder(StreamMessage msg) {
    // Is there already a pending request for the key this message was encrypted with?
    if (msg.getGroupKeyId() != null && pendingGroupKeyRequests.containsKey(msg.getGroupKeyId())) {
      decryptionQueues.add(msg);
    } else {
      // If not, handle normally
      decryptAndHandle(msg);
    }
  }

  private void decryptAndHandle(final StreamMessage msg) {
    try {
      DecryptResult result = tryDecrypt(msg);
      if (result.status) {
        handler.onMessage(this, result.message);

        // Handle new key if the message contains one
        if (msg.getNewGroupKey() != null) {
          keyExchangeUtil.handleNewAESEncryptedKeys(
              Collections.singletonList(msg.getNewGroupKey()),
              msg.getStreamId(),
              msg.getPublisherId(),
              msg.getGroupKeyId());
        }
      } else {
        // If not successfully decrypted, request group key and queue the message
        getLogger()
            .debug(
                "Failed to decrypt stream {} publisher {} ref {}, requesting group key {} and queuing message",
                msg.getStreamId(),
                msg.getPublisherId(),
                msg.getMessageRef(),
                msg.getGroupKeyId());
        requestGroupKeyAndQueueMessage(msg);
      }
    } catch (final UnableToDecryptException e) {
      // failed to decrypt for the second time (after receiving the decryption key(s))
      getLogger()
          .error(
              "Failed to decrypt msg {} from {} in stream {} even after receiving the decryption keys. Calling the onUnableToDecrypt handler!",
              msg.getMessageRef(),
              msg.getPublisherId(),
              msg.getStreamId());
      handler.onUnableToDecrypt(e);
    }
  }

  @Override
  public void onNewKeysAdded(Address publisherId, Collection<GroupKey> groupKeys) {
    // Cancel any pending request timers for all the received keys
    groupKeys.forEach(key -> cancelGroupKeyRequest(key.getGroupKeyId()));

    Set<String> groupKeyIds =
        groupKeys.stream().map(GroupKey::getGroupKeyId).collect(Collectors.toSet());
    Collection<StreamMessage> unlocked =
        decryptionQueues.drainUnlockedMessages(publisherId, groupKeyIds);

    getLogger()
        .trace(
            "Received keys from publisher {}: {}. Unlocked {} queued messages.",
            publisherId,
            groupKeys,
            unlocked.size());

    unlocked.forEach(this::decryptAndHandle);
  }

  public abstract Logger getLogger();

  @FunctionalInterface
  public interface GroupKeyRequestFunction {
    void apply(Address publisherId, List<String> groupKeyIds);
  }
}
