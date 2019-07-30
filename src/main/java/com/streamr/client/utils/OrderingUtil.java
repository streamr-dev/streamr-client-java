package com.streamr.client.utils;

import com.streamr.client.exceptions.GapFillFailedException;
import com.streamr.client.protocol.message_layer.StreamMessage;

import java.util.HashMap;
import java.util.function.Function;

public class OrderingUtil {
    private String streamId;
    private int streamPartition;
    private Function<StreamMessage, Void> inOrderHandler;
    private OrderedMsgChain.GapHandlerFunction gapHandler;
    private Function<GapFillFailedException, Void> gapFillFailedHandler;
    private long gapFillTimeout;
    private HashMap<String, OrderedMsgChain> chains = new HashMap<>();
    public OrderingUtil(String streamId, int streamPartition, Function<StreamMessage, Void> inOrderHandler,
                        OrderedMsgChain.GapHandlerFunction gapHandler, Function<GapFillFailedException, Void> gapFillFailedHandler, long gapFillTimeout) {
        this.streamId = streamId;
        this.streamPartition = streamPartition;
        this.inOrderHandler = inOrderHandler;
        this.gapHandler = gapHandler;
        this.gapFillFailedHandler = gapFillFailedHandler;
        this.gapFillTimeout = gapFillTimeout;
    }
    public OrderingUtil(String streamId, int streamPartition, Function<StreamMessage, Void> inOrderHandler,
                        OrderedMsgChain.GapHandlerFunction gapHandler, long gapFillTimeout) {
        this(streamId, streamPartition, inOrderHandler, gapHandler, (GapFillFailedException e) -> { throw e; }, gapFillTimeout);
    }

    public void add(StreamMessage unorderedMsg) {
        OrderedMsgChain chain = getChain(unorderedMsg.getPublisherId(), unorderedMsg.getMsgChainId());
        chain.add(unorderedMsg);
    }

    public void markMessageExplicitly(StreamMessage msg) {
        OrderedMsgChain chain = getChain(msg.getPublisherId(), msg.getMsgChainId());
        chain.markMessageExplicitly(msg);
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
                    gapHandler, gapFillFailedHandler, gapFillTimeout));
        }
        return chains.get(key);
    }

}
