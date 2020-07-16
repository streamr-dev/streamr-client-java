package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.*;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.OrderedMsgChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

public class CombinedSubscription extends Subscription {

    private static final Logger log = LoggerFactory.getLogger(CombinedSubscription.class);

    private BasicSubscription sub;
    private final ArrayDeque<StreamMessage> queue = new ArrayDeque<>();
    public CombinedSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                Map<String, GroupKey> groupKeys, BasicSubscription.GroupKeyRequestFunction groupKeyRequestFunction,
                                long propagationTimeout, long resendTimeout, boolean skipGapsOnFullQueue) {
        super(streamId, partition, handler, propagationTimeout, resendTimeout, skipGapsOnFullQueue);
        MessageHandler wrapperHandler = new MessageHandler() {
            @Override
            public void onMessage(Subscription sub, StreamMessage message) {
                handler.onMessage(sub, message);
            }
            @Override
            public void done(Subscription s) {
                handler.done(s);

                log.debug("HistoricalSubscription for stream {} is done. Switching to RealtimeSubscription.", streamId);

                // TODO: get group keys from historical subscription?

                // once the initial resend is done, switch to real time
                RealTimeSubscription realTime = new RealTimeSubscription(streamId, partition, handler, groupKeys,
                        groupKeyRequestFunction, propagationTimeout, resendTimeout, skipGapsOnFullQueue);
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
            @Override
            public void onUnableToDecrypt(UnableToDecryptException e) {
                handler.onUnableToDecrypt(e);
            }
        };
        // starts to request the initial resend
        sub = new HistoricalSubscription(streamId, partition, wrapperHandler, resendOption, groupKeys,
                groupKeyRequestFunction, propagationTimeout, resendTimeout, skipGapsOnFullQueue, queue::push);
    }

    @Override
    public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
        sub.setGapHandler(gapHandler);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<GroupKey> groupKeys) throws UnableToSetKeysException {
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
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        sub.handleRealTimeMessage(msg);
    }

    @Override
    public void handleResentMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        sub.handleResentMessage(msg);
    }

    @Override
    public void clear() {
        sub.clear();
    }
}
