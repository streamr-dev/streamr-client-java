package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.OrderingUtil;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Map;

public abstract class BasicSubscription extends Subscription {
    protected OrderingUtil orderingUtil;
    public BasicSubscription(String streamId, int partition, MessageHandler handler, Map<String, String> groupKeysHex,
                             long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, groupKeysHex, propagationTimeout, resendTimeout);
        setOrderingUtil();
    }

    public BasicSubscription(String streamId, int partition, MessageHandler handler, Map<String, String> groupKeysHex) {
        super(streamId, partition, handler, groupKeysHex);
        setOrderingUtil();
    }

    public BasicSubscription(String streamId, int partition, MessageHandler handler) {
        super(streamId, partition, handler);
        setOrderingUtil();
    }

    @Override
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        orderingUtil.add(msg);
    }

    @Override
    public void clear() {
        orderingUtil.clearGaps();
    }

    private void setOrderingUtil() {
        orderingUtil = new OrderingUtil(streamId, partition,
                (StreamMessage msg) -> {
                    decryptAndHandle(msg);
                    return null;
                }, (MessageRef from, MessageRef to, String publisherId, String msgChainId) -> {
            throw new GapDetectedException(streamId, partition, from, to, publisherId, msgChainId);
        }, this.propagationTimeout, this.resendTimeout);
    }

    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        orderingUtil = new OrderingUtil(streamId, partition,
                (StreamMessage msg) -> {
                    decryptAndHandle(msg);
                    return null;
                }, gapHandler, propagationTimeout, resendTimeout);
    }

    public OrderedMsgChain.GapHandlerFunction getGapHandler() {
        return orderingUtil.getGapHandler();
    }

    private void decryptAndHandle(StreamMessage msg) {
        SecretKey newGroupKey = EncryptionUtil.decryptStreamMessage(msg, groupKeys.get(msg.getPublisherId()));
        if (newGroupKey != null) {
            groupKeys.put(msg.getPublisherId(), newGroupKey);
        }
        handler.onMessage(null, msg);
    }

    public void setLastMessageRefs(ArrayList<OrderedMsgChain> chains) {
        orderingUtil.setLastMessageRefs(chains);
    }

    public ArrayList<OrderedMsgChain> getChains() {
        return orderingUtil.getChains();
    }
}
