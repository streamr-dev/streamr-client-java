package com.streamr.client.utils;

import com.streamr.client.exceptions.GapFillFailedException;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class OrderingUtil {
    private String streamId;
    private int streamPartition;
    private Consumer<StreamMessage> inOrderHandler;
    private OrderedMsgChain.GapHandlerFunction gapHandler;
    private Function<GapFillFailedException, Void> gapFillFailedHandler;
    private long propagationTimeout;
    private long resendTimeout;
    private HashMap<String, OrderedMsgChain> chains = new HashMap<>();
    public OrderingUtil(String streamId, int streamPartition, Consumer<StreamMessage> inOrderHandler,
                        OrderedMsgChain.GapHandlerFunction gapHandler, Function<GapFillFailedException, Void> gapFillFailedHandler, long propagationTimeout, long resendTimeout) {
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.inOrderHandler = inOrderHandler;
        this.gapHandler = gapHandler;
        this.gapFillFailedHandler = gapFillFailedHandler;
        this.propagationTimeout = propagationTimeout;
        this.resendTimeout = resendTimeout;
    }
    public OrderingUtil(String streamId, int streamPartition, Consumer<StreamMessage> inOrderHandler,
                        OrderedMsgChain.GapHandlerFunction gapHandler, long propagationTimeout, long resendTimeout) {
        this(streamId, streamPartition, inOrderHandler, gapHandler, (GapFillFailedException e) -> { throw e; }, propagationTimeout, resendTimeout);
    }

    public void add(StreamMessage unorderedMsg) {
        OrderedMsgChain chain = getChain(unorderedMsg.getPublisherId(), unorderedMsg.getMsgChainId());
        chain.add(unorderedMsg);
    }

    public void clearGaps() {
        for (OrderedMsgChain chain: chains.values()) {
            chain.clearGap();
        }
    }

    private OrderedMsgChain getChain(String publisherId, String msgChainId) {
        String key = publisherId + msgChainId;
        if (!chains.containsKey(key)) {
            chains.put(key, new OrderedMsgChain(publisherId, msgChainId, inOrderHandler,
                    gapHandler, gapFillFailedHandler, propagationTimeout, resendTimeout));
        }
        return chains.get(key);
    }

    public ArrayList<OrderedMsgChain> getChains() {
        return new ArrayList<>(chains.values());
    }

    public OrderedMsgChain.GapHandlerFunction getGapHandler() {
        return gapHandler;
    }

    public void addChains(ArrayList<OrderedMsgChain> previousChains) {
        for (OrderedMsgChain chain: previousChains) {
            String key = chain.getPublisherId() + chain.getMsgChainId();
            OrderedMsgChain newChain = new OrderedMsgChain(chain.getPublisherId(), chain.getMsgChainId(),
                    inOrderHandler, gapHandler, gapFillFailedHandler, propagationTimeout, resendTimeout);
            newChain.setLastReceived(chain.getLastReceived());
            chains.put(key, newChain);

        }
    }

}
