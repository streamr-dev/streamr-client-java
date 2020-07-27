package com.streamr.client.utils;

import com.streamr.client.protocol.message_layer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility class for queuing encrypted messages while waiting for their
 * decryption keys to be received. The messages are queued by [publisherId, msgChainId] tuples.
 */
public class DecryptionQueues {
    private static final Logger log = LoggerFactory.getLogger(DecryptionQueues.class);

    private final Map<Address, Map<String, ArrayDeque<StreamMessage>>> msgChainsByPublisher = new HashMap<>();
    private final String streamId;
    private final int partition;

    public DecryptionQueues(String streamId, int partition) {
        this.streamId = streamId;
        this.partition = partition;
    }

    private Map<String, ArrayDeque<StreamMessage>> getQueuesByMsgChain(Address publisherId) {
        if (!msgChainsByPublisher.containsKey(publisherId)) {
            msgChainsByPublisher.put(publisherId, new LinkedHashMap<>());
        }
        return msgChainsByPublisher.get(publisherId);
    }

    public ArrayDeque<StreamMessage> getQueue(Address publisherId, String msgChainId) {
        Map<String, ArrayDeque<StreamMessage>> queuesByMsgChain = getQueuesByMsgChain(publisherId);
        if (!queuesByMsgChain.containsKey(msgChainId)) {
            queuesByMsgChain.put(msgChainId, new ArrayDeque<>());
        }
        return queuesByMsgChain.get(msgChainId);
    }

    public void add(StreamMessage msg) {
        getQueue(msg.getPublisherId(), msg.getMsgChainId()).offer(msg);
        log.trace("Message added to encryption queue: stream {}, partition {}, publisher {}, msgChain {}, ref {}",
                msg.getStreamId(), msg.getStreamPartition(), msg.getPublisherId(), msg.getMsgChainId(), msg.getMessageRef());
    }

    /**
     * Returns a list of messages that can now be processed thanks to the provided groupKeyIds.
     * Returned messages are removed from the queue.
     */
    public Collection<StreamMessage> drainUnlockedMessages(Address publisherId, Set<String> groupKeyIds) {
        Map<String, ArrayDeque<StreamMessage>> queuesByMsgChainId = getQueuesByMsgChain(publisherId);
        List<StreamMessage> unlockedMessages = new ArrayList<>();
        Set<String> msgChainsWithEmptyLists = new HashSet<>();

        // Find queues where the next messages can be processed with one of the provided groupKeys
        for (Map.Entry<String, ArrayDeque<StreamMessage>> entry : queuesByMsgChainId.entrySet()) {
            String msgChainId = entry.getKey();
            ArrayDeque<StreamMessage> queue = entry.getValue();

            log.trace("Checking encryption queue for stream {}, partition {}, publisher {}, msgChain {}. Queue size: {}",
                    streamId, partition, publisherId, msgChainId, queue.size());

            // Move processable messages from the queue to the result list
            while (!queue.isEmpty() && groupKeyIds.contains(queue.peek().getGroupKeyId())) {
                unlockedMessages.add(queue.poll());
            }

            // Clean up if the queue became empty to prevent memory leak
            if (queue.isEmpty()) {
                msgChainsWithEmptyLists.add(msgChainId);
            }
        }

        // Clean up empty branches of the structure
        for (String msgChainId : msgChainsWithEmptyLists) {
            queuesByMsgChainId.remove(msgChainId);
        }
        if (queuesByMsgChainId.isEmpty()) {
            msgChainsByPublisher.remove(publisherId);
        }

        return unlockedMessages;
    }

    public boolean isEmpty() {
        return msgChainsByPublisher.isEmpty();
    }
}
