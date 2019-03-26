package com.streamr.client;

import com.streamr.client.options.ResendFromOption;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.StreamPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

public class Subscription {

    private static final Logger log = LogManager.getLogger();

    private final String id;
    private final StreamPartition streamPartition;
    private final MessageHandler handler;
    private ResendOption resendOption = null;

    private final HashMap<String, MessageRef> lastReceivedMsgRef = new HashMap<>();
    private boolean resending = false;
    private final LinkedList<StreamMessage> queue = new LinkedList<>();

    private State state;

    enum State {
        SUBSCRIBING, SUBSCRIBED, UNSUBSCRIBING, UNSUBSCRIBED
    }

    public Subscription(String streamId, int partition, MessageHandler handler) {
        this.id = UUID.randomUUID().toString();
        this.streamPartition = new StreamPartition(streamId, partition);
        this.handler = handler;
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this.id = UUID.randomUUID().toString();
        this.streamPartition = new StreamPartition(streamId, partition);
        this.handler = handler;
        this.resendOption = resendOption;
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

    public MessageHandler getHandler() {
        return handler;
    }

    public boolean isResending() {
        return resending;
    }

    public void setResending(boolean resending) {
        this.resending = resending;
    }

    public ResendRangeRequest handleMessage(StreamMessage msg, String sessionToken) {
        return handleMessage(msg, sessionToken, false);
    }

    public ResendRangeRequest handleMessage(StreamMessage msg, String sessionToken, boolean isResend) {
        String key = msg.getPublisherId() + msg.getMsgChainId();
        if (resending && !isResend) {
            queue.add(msg);
            return null;
        } else if (checkForGap(msg.getPreviousMessageRef(), key) && !resending) {
            queue.add(msg);
            MessageRef from = lastReceivedMsgRef.get(key); // cannot know the first missing message so there will be a duplicate received
            MessageRef to = msg.getPreviousMessageRef();
            return new ResendRangeRequest(msg.getStreamId(), msg.getStreamPartition(), id, from, to, msg.getPublisherId(), msg.getMsgChainId(), sessionToken);
        } else {
            MessageRef msgRef = msg.getMessageRef();
            Integer res = null;
            MessageRef last = lastReceivedMsgRef.get(key);
            if (last != null) {
                res = msgRef.compareTo(last);
            }
            if (res != null && res <= 0) {
                log.debug(String.format("Sub %s already received message: %s, lastReceivedMsgRef: %s. Ignoring message.", id, msgRef, last));
            } else {
                lastReceivedMsgRef.put(key, msgRef);
                getHandler().onMessage(this, msg);
            }
            return null;
        }
    }

    public ResendRangeRequest handleQueue(String sessionToken) {
        ResendRangeRequest gap = null;
        while(!queue.isEmpty() && gap == null) {
            StreamMessage msg = queue.poll();
            gap = handleMessage(msg, sessionToken);
        }
        return gap;
    }

    public void handleError(Exception e, StreamMessage msg) {
        String key = msg.getPublisherId() + msg.getMsgChainId();
        if (e instanceof IOException) {
            if(!checkForGap(msg.getPreviousMessageRef(), key)) {
                lastReceivedMsgRef.put(key, msg.getMessageRef());
            }
        } else {
            throw new RuntimeException(e);
        }
    }

    public boolean hasResendOptions() {
        return resendOption != null;
    }

    /**
     * Resend needs can change if messages have already been received.
     * This function always returns the effective resend options:
     *
     * If messages have been received and 'resendOption' is a ResendFromOption,
     * then it is updated with the latest received message.
     */
    public ResendOption getEffectiveResendOption() {
        if (resendOption instanceof ResendFromOption) {
            ResendFromOption resendFromOption = (ResendFromOption) resendOption;
            if (resendFromOption.getPublisherId() != null && resendFromOption.getMsgChainId() != null) {
                String key = resendFromOption.getPublisherId() + resendFromOption.getMsgChainId();
                MessageRef last = lastReceivedMsgRef.get(key);
                if (last != null) {
                    return new ResendFromOption(last, resendFromOption.getPublisherId(), resendFromOption.getMsgChainId());
                }
            }
        }
        return resendOption;
    }

    private boolean checkForGap(MessageRef prev, String key) {
        if (prev == null) {
            return false;
        }
        MessageRef last = lastReceivedMsgRef.get(key);
        return last != null && prev.compareTo(last) > 0;
    }
}
