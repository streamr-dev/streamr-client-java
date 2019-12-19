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
    private static final Logger log = LogManager.getLogger();

    protected OrderingUtil orderingUtil;
    protected final HashSet<String> waitingForGroupKey = new HashSet<>();
    protected final ArrayDeque<StreamMessage> encryptedMsgsQueue = new ArrayDeque<>();
    protected final GroupKeyRequestFunction groupKeyRequestFunction;
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
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
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

    protected void handleInOrder(StreamMessage msg) {
        if (!waitingForGroupKey.contains(msg.getPublisherId())) {
            boolean success = decryptOrRequestGroupKey(msg);
            if (success) { // the message was successfully decrypted
                handler.onMessage(this, msg);
            }
        } else {
            encryptedMsgsQueue.offer(msg);
        }
    }

    public ArrayList<OrderedMsgChain> getChains() {
        return orderingUtil.getChains();
    }

    public abstract boolean decryptOrRequestGroupKey(StreamMessage msg);

    @FunctionalInterface
    public interface GroupKeyRequestFunction {
        void apply(String publisherId, Date start, Date end);
    }
}
