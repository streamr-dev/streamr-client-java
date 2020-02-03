package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.OrderingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public abstract class BasicSubscription extends Subscription {
    protected static final Logger log = LogManager.getLogger();

    protected OrderingUtil orderingUtil;
    private final HashSet<String> waitingForGroupKey = new HashSet<>();
    protected final ArrayDeque<StreamMessage> encryptedMsgsQueue = new ArrayDeque<>();
    private final GroupKeyRequestFunction groupKeyRequestFunction;
    public BasicSubscription(String streamId, int partition, MessageHandler handler,
                             GroupKeyRequestFunction groupKeyRequestFunction, long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, propagationTimeout, resendTimeout);
        orderingUtil = new OrderingUtil(streamId, partition,
                this::handleInOrder, (MessageRef from, MessageRef to, String publisherId, String msgChainId) -> {
            throw new GapDetectedException(streamId, partition, from, to, publisherId, msgChainId);
        }, this.propagationTimeout, this.resendTimeout);
        this.groupKeyRequestFunction = groupKeyRequestFunction != null ? groupKeyRequestFunction
                : ((publisherId, start, end) -> log.warn("Group key missing for stream " + streamId + " and publisher " + publisherId + " but no handler is set."));
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
                this::handleInOrder, gapHandler, propagationTimeout, resendTimeout);
    }

    public OrderedMsgChain.GapHandlerFunction getGapHandler() {
        return orderingUtil.getGapHandler();
    }

    protected void requestGroupKeyAndQueueMessage(StreamMessage msgToQueue, Date start, Date end) {
        groupKeyRequestFunction.apply(msgToQueue.getPublisherId(), start, end);
        waitingForGroupKey.add(msgToQueue.getPublisherId());
        encryptedMsgsQueue.offer(msgToQueue);
    }

    protected void handleInOrderQueue(String publisherId) {
        waitingForGroupKey.remove(publisherId);
        while (!encryptedMsgsQueue.isEmpty() && !waitingForGroupKey.contains(encryptedMsgsQueue.peek().getPublisherId())) {
            decryptAndHandle(encryptedMsgsQueue.poll());
        }
    }

    public ArrayList<OrderedMsgChain> getChains() {
        return orderingUtil.getChains();
    }

    public abstract boolean decryptOrRequestGroupKey(StreamMessage msg) throws UnableToDecryptException;

    private void handleInOrder(StreamMessage msg) {
        if (!waitingForGroupKey.contains(msg.getPublisherId())) {
            decryptAndHandle(msg);
        } else {
            encryptedMsgsQueue.offer(msg);
        }
    }

    private void decryptAndHandle(StreamMessage msg) {
        try {
            boolean success = decryptOrRequestGroupKey(msg);
            if (success) { // the message was successfully decrypted
                handler.onMessage(this, msg);
            } else {
                log.warn("Failed to decrypt. Requested the correct decryption key(s) and going to try again.");
            }
        } catch (UnableToDecryptException e) { // failed to decrypt for the second time (after receiving the decryption key(s))
            handler.onUnableToDecrypt(e);
        }
    }

    @FunctionalInterface
    public interface GroupKeyRequestFunction {
        void apply(String publisherId, Date start, Date end);
    }
}
