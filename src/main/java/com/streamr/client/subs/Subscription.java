package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.*;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.UnencryptedGroupKey;

import java.util.*;

public abstract class Subscription {
    public static final long DEFAULT_PROPAGATION_TIMEOUT = 5000L;
    public static final long DEFAULT_RESEND_TIMEOUT = 5000L;

    protected final String streamId;
    protected final int partition;
    private final String id;
    protected final MessageHandler handler;
    protected final long propagationTimeout;
    protected final long resendTimeout;

    private State state;

    public enum State {
        SUBSCRIBING, SUBSCRIBED, UNSUBSCRIBING, UNSUBSCRIBED
    }

    public Subscription(String streamId, int partition, MessageHandler handler,
                        long propagationTimeout, long resendTimeout) {
        this.id = UUID.randomUUID().toString();
        this.streamId = streamId;
        this.partition = partition;
        this.handler = handler;
        this.propagationTimeout = propagationTimeout;
        this.resendTimeout = resendTimeout;
    }

    public String getId() {
        return id;
    }

    public String getStreamId() {
        return streamId;
    }

    public int getPartition() {
        return partition;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isSubscribed() {
        return state.equals(State.SUBSCRIBED);
    }

    public abstract boolean isResending();

    public abstract void setResending(boolean resending);

    public abstract boolean hasResendOptions();

    public abstract ResendOption getResendOption();

    public abstract void startResend();

    public abstract void endResend() throws GapDetectedException;

    public abstract void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException;

    public abstract void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException;

    public abstract void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler);

    public abstract void setGroupKeys(String publisherId, ArrayList<UnencryptedGroupKey> groupKeys) throws UnableToSetKeysException;

    public abstract void clear();
}
