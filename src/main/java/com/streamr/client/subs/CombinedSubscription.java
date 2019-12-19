package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.UnencryptedGroupKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

public class CombinedSubscription extends Subscription {
    private BasicSubscription sub;
    private final ArrayDeque<StreamMessage> queue = new ArrayDeque<>();
    public CombinedSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                Map<String, UnencryptedGroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction,
                                long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, propagationTimeout, resendTimeout);
        MessageHandler wrapperHandler = new MessageHandler() {
            @Override
            public void onMessage(Subscription sub, StreamMessage message) {
                handler.onMessage(sub, message);
            }
            @Override
            public void done(Subscription s) {
                handler.done(s);
                // once the initial resend is done, switch to real time
                RealTimeSubscription realTime = new RealTimeSubscription(streamId, partition, handler, groupKeys, groupKeyRequestFunction, propagationTimeout, resendTimeout);
                realTime.setGapHandler(sub.getGapHandler());
                // set the last received references to the last references of the resent messages
                realTime.setLastMessageRefs(sub.getChains());
                // handle the real time messages received during the initial resend
                while(!queue.isEmpty()) {
                    StreamMessage msg = queue.poll();
                    realTime.handleRealTimeMessage(msg);
                }
                sub = realTime;
            }
        };
        // starts to request the initial resend
        sub = new HistoricalSubscription(streamId, partition, wrapperHandler, resendOption, groupKeys, groupKeyRequestFunction, propagationTimeout, resendTimeout, queue::push);
    }

    @Override
    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        sub.setGapHandler(gapHandler);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<UnencryptedGroupKey> groupKeys) {
        sub.setGroupKeys(publisherId, groupKeys);
    }

    @Override
    public boolean isResending() {
        return sub.isResending();
    }

    @Override
    public void setResending(boolean resending) {
        sub.setResending(resending);
    }

    @Override
    public boolean hasResendOptions() {
        return sub.hasResendOptions();
    }

    @Override
    public ResendOption getResendOption() {
        return sub.getResendOption();
    }

    @Override
    public void startResend() {
        sub.startResend();
    }

    @Override
    public void endResend() throws GapDetectedException {
        sub.endResend();
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        sub.handleRealTimeMessage(msg);
    }

    @Override
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        sub.handleResentMessage(msg);
    }

    @Override
    public void clear() {
        sub.clear();
    }
}
