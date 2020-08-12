package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashSet;

public class RealTimeSubscription extends BasicSubscription {

    private static final Logger log = LoggerFactory.getLogger(RealTimeSubscription.class);

    private boolean resending = false;

    public RealTimeSubscription(String streamId,
                                int partition,
                                MessageHandler handler,
                                GroupKeyStore keyStore,
                                KeyExchangeUtil keyExchangeUtil,
                                GroupKeyRequestFunction groupKeyRequestFunction,
                                long propagationTimeout,
                                long resendTimeout,
                                boolean skipGapsOnFullQueue) {
        super(streamId, partition, handler, keyStore, keyExchangeUtil, groupKeyRequestFunction, propagationTimeout, resendTimeout,
                skipGapsOnFullQueue);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, GroupKeyStore keyStore, KeyExchangeUtil keyExchangeUtil,
                                GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, keyStore, keyExchangeUtil, groupKeyRequestFunction, Subscription.DEFAULT_PROPAGATION_TIMEOUT,
                Subscription.DEFAULT_RESEND_TIMEOUT, Subscription.DEFAULT_SKIP_GAPS_ON_FULL_QUEUE);
    }

    @Override
    public boolean isResending() {
        return resending;
    }

    @Override
    public void setResending(boolean resending) {
        this.resending = resending;
    }

    @Override
    public boolean hasResendOptions() {
        return false;
    }

    @Override
    public ResendOption getResendOption() {
        return null;
    }

    @Override
    public void startResend() {
        resending = true;
    }

    @Override
    public void endResend() throws GapDetectedException {
        resending = false;
        this.handler.done(this);
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        orderingUtil.add(msg);
    }

    public void setLastMessageRefs(ArrayList<OrderedMsgChain> chains) {
        orderingUtil.addChains(chains);
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}
