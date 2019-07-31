package com.streamr.client.utils;

import com.streamr.client.exceptions.GapFillFailedException;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

public class OrderedMsgChain {
    private static final Logger log = LogManager.getLogger();
    private static final int MAX_GAP_REQUESTS = 10;

    private String publisherId;
    private String msgChainId;
    private Function<StreamMessage, Void> inOrderHandler;
    private GapHandlerFunction gapHandler;
    private Function<GapFillFailedException, Void> gapFillFailedHandler;
    private long propagationTimeout;
    private long resendTimeout;
    private PriorityQueue<StreamMessage> queue;
    private MessageRef lastReceived = null;
    private Timer gap = null;
    private int gapRequestCount = 0;
    private GapFillFailedException gapException = null;

    public OrderedMsgChain(String publisherId, String msgChainId, Function<StreamMessage, Void> inOrderHandler,
                           GapHandlerFunction gapHandler, Function<GapFillFailedException, Void> gapFillFailedHandler, long propagationTimeout, long resendTimeout) {
        this.publisherId = publisherId;
        this.msgChainId = msgChainId;
        this.inOrderHandler = inOrderHandler;
        this.gapHandler = gapHandler;
        this.gapFillFailedHandler = gapFillFailedHandler;
        this.propagationTimeout = propagationTimeout;
        this.resendTimeout = resendTimeout;
        queue = new PriorityQueue<>(new Comparator<StreamMessage>() {
            @Override
            public int compare(StreamMessage o1, StreamMessage o2) {
                return o1.getMessageRef().compareTo(o2.getMessageRef());
            }
        });
    }
    public OrderedMsgChain(String publisherId, String msgChainId, Function<StreamMessage, Void> inOrderHandler,
                           GapHandlerFunction gapHandler, long propagationTimeout, long resendTimeout) {
        this(publisherId, msgChainId, inOrderHandler, gapHandler, (GapFillFailedException e) -> { throw e; }, propagationTimeout, resendTimeout);
    }

    public void add(StreamMessage unorderedMsg) {
        MessageRef ref = unorderedMsg.getMessageRef();
        if (lastReceived != null && ref.compareTo(lastReceived) <= 0) {
            log.debug("Already received message: " + ref + ", lastReceivedMsgRef: " + lastReceived + ". Ignoring message.");
            return;
        }
        if (isNextMessage(unorderedMsg)) {
            process(unorderedMsg);
            checkQueue();
        } else {
            if (gap == null) {
                scheduleGap();
            }
            queue.offer(unorderedMsg);
        }
    }

    public void clearGap() {
        if (gap != null) {
            gap.cancel();
            gap = null;
            if (gapException != null) {
                throw gapException;
            }
        }
    }

    private boolean isNextMessage(StreamMessage msg) {
        boolean isFirstMessage = lastReceived == null;
        return isFirstMessage
            // is chained and next
            || (msg.getPreviousMessageRef() != null && msg.getPreviousMessageRef().compareTo(lastReceived) == 0)
            // is unchained and newer
            || (msg.getPreviousMessageRef() == null && msg.getMessageRef().compareTo(lastReceived) > 0);
    }

    private void checkQueue() {
        while(!queue.isEmpty()) {
            StreamMessage msg = queue.peek();
            if (msg !=null && isNextMessage(msg)) {
                queue.poll();
                // If the next message is found in the queue, any gap must have been filled, so clear the timer
                clearGap();
                process(msg);
            } else {
                return;
            }
        }
    }

    private void process(StreamMessage msg) {
        lastReceived = msg.getMessageRef();
        inOrderHandler.apply(msg);
    }

    private void scheduleGap() {
        gapRequestCount = 0;
        gap = new Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                MessageRef from = new MessageRef(lastReceived.getTimestamp(), lastReceived.getSequenceNumber() + 1);
                MessageRef to = queue.peek().getPreviousMessageRef();
                if (gapRequestCount < MAX_GAP_REQUESTS) {
                    gapRequestCount++;
                    gapHandler.apply(from, to, publisherId, msgChainId);
                } else {
                    gapFillFailedHandler.apply(new GapFillFailedException(from, to, publisherId, msgChainId, MAX_GAP_REQUESTS));
                    clearGap();
                }
            }
        };
        gap.schedule(task, propagationTimeout, resendTimeout);
    }

    @FunctionalInterface
    public interface GapHandlerFunction {
        void apply(MessageRef from, MessageRef to, String publisherId, String msgChainId);
    }
}
