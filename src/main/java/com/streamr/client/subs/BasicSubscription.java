package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasicSubscription extends Subscription {
    public static final int MAX_NB_GROUP_KEY_REQUESTS = 10;

    protected OrderingUtil orderingUtil;
    private final ConcurrentHashMap<String, Timer> pendingGroupKeyRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nbGroupKeyRequestsCalls = new ConcurrentHashMap<>();
    private final HashSet<String> alreadyFailedToDecrypt = new HashSet<>();
    protected class MsgQueues {
        private final HashMap<String, ArrayDeque<StreamMessage>> queues = new HashMap<>();

        public ArrayDeque<StreamMessage> get(String publisherId) {
            if (!queues.containsKey(publisherId.toLowerCase())) {
                queues.put(publisherId.toLowerCase(), new ArrayDeque<>());
            }
            return queues.get(publisherId.toLowerCase());
        }

        public void offer(StreamMessage msg) {
            get(msg.getPublisherId()).offer(msg);
            getLogger().trace("Message added to encryption queue: {}", msg.getMessageRef());
        }

        public boolean isEmpty() {
            for (ArrayDeque<StreamMessage> queue: queues.values()) {
                if (!queue.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }
    protected final MsgQueues encryptedMsgsQueues = new MsgQueues();
    private final GroupKeyRequestFunction groupKeyRequestFunction;
    public BasicSubscription(String streamId, int partition, MessageHandler handler, GroupKeyStore keyStore,
                             GroupKeyRequestFunction groupKeyRequestFunction, long propagationTimeout,
                             long resendTimeout, boolean skipGapsOnFullQueue) {
        super(streamId, partition, handler, keyStore, propagationTimeout, resendTimeout, skipGapsOnFullQueue);

        orderingUtil = new OrderingUtil(
                streamId, partition,
                this::handleInOrder,
                (MessageRef from, MessageRef to, String publisherId, String msgChainId) -> {
                    throw new GapDetectedException(streamId, partition, from, to, publisherId, msgChainId);
                },
                this.propagationTimeout, this.resendTimeout, this.skipGapsOnFullQueue
        );

        this.groupKeyRequestFunction = groupKeyRequestFunction != null ? groupKeyRequestFunction
                : ((publisherId, groupKeyIds) -> getLogger().warn("Group key missing for stream " + streamId + " and publisher " + publisherId + " but no handler is set."));
    }

    @Override
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        orderingUtil.add(msg);
    }

    @Override
    public void clear() {
        orderingUtil.clearGaps();
    }

    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        orderingUtil = new OrderingUtil(streamId, partition,
                this::handleInOrder, gapHandler, propagationTimeout, resendTimeout, skipGapsOnFullQueue);
    }

    public OrderedMsgChain.GapHandlerFunction getGapHandler() {
        return orderingUtil.getGapHandler();
    }

    protected void requestGroupKeyAndQueueMessage(StreamMessage msgToQueue) {
        Timer t = new Timer(String.format("GroupKeyTimer-%s-%s", msgToQueue.getStreamId(), msgToQueue.getMessageRef().toString()), true);
        String publisherId = msgToQueue.getPublisherId().toLowerCase();
        nbGroupKeyRequestsCalls.put(publisherId, 0);
        TimerTask request = new TimerTask() {
            @Override
            public void run() {
                synchronized (BasicSubscription.this) {
                    if (pendingGroupKeyRequests.containsKey(publisherId)) {
                        if (nbGroupKeyRequestsCalls.get(publisherId) < MAX_NB_GROUP_KEY_REQUESTS) {
                            nbGroupKeyRequestsCalls.put(publisherId, nbGroupKeyRequestsCalls.get(publisherId) + 1);
                            groupKeyRequestFunction.apply(publisherId, Collections.singletonList(msgToQueue.getGroupKeyId()));
                            getLogger().info("Sent key request for stream {} publisher {}, key id {}",
                                    streamId, publisherId, msgToQueue.getGroupKeyId());
                        } else {
                            getLogger().warn("Failed to receive group key response from "
                                    + publisherId + " after " + MAX_NB_GROUP_KEY_REQUESTS + " requests.");
                            cancelGroupKeyRequest(publisherId);
                        }
                    }
                }
            }
        };
        t.schedule(request, 0, propagationTimeout);
        pendingGroupKeyRequests.put(publisherId, t);
        encryptedMsgsQueues.offer(msgToQueue);
    }

    protected void handleInOrderQueue(String publisherId) {
        cancelGroupKeyRequest(publisherId);
        ArrayDeque<StreamMessage> queue = encryptedMsgsQueues.get(publisherId);
        getLogger().trace("Checking and handling encryption queue for publisher {}. Queue size: {}", publisherId, queue.size());
        while (!queue.isEmpty()) {
            decryptAndHandle(queue.poll());
        }
    }

    private synchronized void cancelGroupKeyRequest(String publisherId) {
        publisherId = publisherId.toLowerCase();
        if (pendingGroupKeyRequests.containsKey(publisherId)) {
            getLogger().trace("Pending group key request canceled for publisher {}", publisherId);
            Timer timer = pendingGroupKeyRequests.get(publisherId);
            timer.cancel();
            timer.purge();
            pendingGroupKeyRequests.remove(publisherId);
        }
    }

    public ArrayList<OrderedMsgChain> getChains() {
        return orderingUtil.getChains();
    }

    private boolean decryptOrRequestGroupKey(StreamMessage msg) throws UnableToDecryptException {
        if (msg.getEncryptionType() == StreamMessage.EncryptionType.NONE) {
            return true;
        }

        try {
            GroupKey groupKey = keyStore.get(msg.getStreamId(), msg.getGroupKeyId());
            if (groupKey == null) {
                throw new UnableToDecryptException(msg.getSerializedContent());
            }

            EncryptionUtil.decryptStreamMessage(msg, groupKey.toSecretKey());
            alreadyFailedToDecrypt.remove(msg.getPublisherId());
            return true;
        } catch (UnableToDecryptException e) {
            if (alreadyFailedToDecrypt.contains(msg.getPublisherId())) {
                // even after receiving the latest group key, we still cannot decrypt
                throw e;
            } else {
                // Fail next time we come here
                alreadyFailedToDecrypt.add(msg.getPublisherId());
            }

            // we will queue real time messages while waiting for the group key (including this one
            // since it could not be decrypted)
            getLogger().debug("Failed to decrypt {}, requesting group key and queuing message", msg.getMessageRef());
            requestGroupKeyAndQueueMessage(msg);
            return false;
        }
    }

    private void handleInOrder(StreamMessage msg) {
        if (!pendingGroupKeyRequests.containsKey(msg.getPublisherId().toLowerCase())) {
            decryptAndHandle(msg);
        } else {
            encryptedMsgsQueues.offer(msg);
        }
    }

    private void decryptAndHandle(StreamMessage msg) {
        try {
            boolean success = decryptOrRequestGroupKey(msg);
            if (success) { // the message was successfully decrypted
                handler.onMessage(this, msg);
            } else {
                getLogger().info("Failed to decrypt msg {} from {}. Going to request the correct decryption key(s) and try again.",
                        msg.getMessageRef(), msg.getPublisherId());
            }
        } catch (UnableToDecryptException e) { // failed to decrypt for the second time (after receiving the decryption key(s))
            getLogger().error("Failed to decrypt msg {} from {} even after receiving the decryption keys.",
                    msg.getMessageRef(), msg.getPublisherId());
            handler.onUnableToDecrypt(e);
        }
    }

    @Override
    public void onNewKeys(String publisherId, Collection<GroupKey> groupKeys) {
        // handle real time messages received while waiting for the group key
        handleInOrderQueue(publisherId);
    }

    public abstract Logger getLogger();

    @FunctionalInterface
    public interface GroupKeyRequestFunction {
        void apply(String publisherId, List<String> groupKeyIds);
    }
}
