package com.streamr.client;

import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.*;

public class Subscription {
    private static final Logger log = LogManager.getLogger();
    public static final long DEFAULT_PROPAGATION_TIMEOUT = 5000L;
    public static final long DEFAULT_RESEND_TIMEOUT = 5000L;

    private final String id;
    private final StreamPartition streamPartition;
    private final MessageHandler handler;
    private ResendOption resendOption;
    private final Map<String, SecretKey> groupKeys = new HashMap<>(); // publisherId --> groupKey

    private boolean resending = false;
    private final ArrayDeque<StreamMessage> queue = new ArrayDeque<>();
    private OrderingUtil orderingUtil;
    private final long propagationTimeout;
    private final long resendTimeout;

    private State state;

    enum State {
        SUBSCRIBING, SUBSCRIBED, UNSUBSCRIBING, UNSUBSCRIBED
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, GroupKey> groupKeys,
                        long propagationTimeout, long resendTimeout) {
        this.id = UUID.randomUUID().toString();
        this.streamPartition = new StreamPartition(streamId, partition);
        this.handler = handler;
        this.resendOption = resendOption;
        if (groupKeys != null) {
            for (String publisherId: groupKeys.keySet()) {
                String groupKeyHex = groupKeys.get(publisherId).getGroupKeyHex();
                this.groupKeys.put(publisherId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
            }
        }
        orderingUtil = new OrderingUtil(streamId, partition,
                (StreamMessage msg) -> {
                    decryptAndHandle(msg);
                    return null;
                }, (MessageRef from, MessageRef to, String publisherId, String msgChainId) -> {
            throw new GapDetectedException(streamId, partition, from, to, publisherId, msgChainId);
        }, propagationTimeout, resendTimeout);
        this.propagationTimeout = propagationTimeout;
        this.resendTimeout = resendTimeout;
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, GroupKey> groupKeys) {
        this(streamId, partition, handler, resendOption, groupKeys, DEFAULT_PROPAGATION_TIMEOUT, DEFAULT_RESEND_TIMEOUT);
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this(streamId, partition, handler, resendOption, null);
    }

    public Subscription(String streamId, int partition, MessageHandler handler) {
        this(streamId, partition, handler, null, null);
    }

    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        orderingUtil = new OrderingUtil(streamPartition.getStreamId(), streamPartition.getPartition(),
                (StreamMessage msg) -> {
                    decryptAndHandle(msg);
                    return null;
                }, gapHandler, propagationTimeout, resendTimeout);
    }

    public String getId() {
        return id;
    }

    public String getStreamId() {
        return streamPartition.getStreamId();
    }

    public int getPartition() {
        return streamPartition.getPartition();
    }

    public StreamPartition getStreamPartition() {
        return streamPartition;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isResending() {
        return resending;
    }

    public void setResending(boolean resending) {
        this.resending = resending;
    }

    public void handleMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        handleMessage(msg, false);
    }

    public void handleMessage(StreamMessage msg, boolean isResend) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        // we queue real-time messages until the initial resend (subscribe with resend options) is completed.
        if(hasResendOptions() && !isResend) {
            queue.add(msg);
        } else {
            orderingUtil.add(msg);
        }
    }

    public boolean hasResendOptions() {
        return resendOption != null;
    }

    public ResendOption getResendOption() {
        return resendOption;
    }

    public void startResend() {
        resending = true;
    }

    public void endResend() throws GapDetectedException {
        resending = false;
        resendOption = null;
        handleQueue();
    }

    public boolean isSubscribed() {
        return state.equals(State.SUBSCRIBED);
    }

    void resendDone() {
        if (this.resending) {
            log.error("Resending should be done");
        }

        this.handler.done(this);
    }

    public void clear() {
        orderingUtil.clearGaps();
    }

    private void handleQueue() throws GapDetectedException {
        while(!queue.isEmpty()) {
            StreamMessage msg = queue.poll();
            handleMessage(msg);
        }
    }

    private void decryptAndHandle(StreamMessage msg) {
        SecretKey newGroupKey = EncryptionUtil.decryptStreamMessage(msg, groupKeys.get(msg.getPublisherId()));
        if (newGroupKey != null) {
            groupKeys.put(msg.getPublisherId(), newGroupKey);
        }
        handler.onMessage(this, msg);
    }
}
