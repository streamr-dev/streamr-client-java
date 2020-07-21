package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.GroupKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Consumer;

public class HistoricalSubscription extends BasicSubscription {

    private static final Logger log = LoggerFactory.getLogger(HistoricalSubscription.class);

    private final ResendOption resendOption;
    private boolean resendDone = false;
    private final Consumer<StreamMessage> onRealTimeMsg;

    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  GroupKeyStore keyStore,
                                  ResendOption resendOption,
                                  GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout,
                                  long resendTimeout,
                                  boolean skipGapsOnFullQueue,
                                  Consumer<StreamMessage> onRealTimeMsg) {
        super(streamId, partition, handler, keyStore, groupKeyRequestFunction, propagationTimeout, resendTimeout,
                skipGapsOnFullQueue);
        this.resendOption = resendOption;
        this.onRealTimeMsg = onRealTimeMsg;
    }

    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  GroupKeyStore keyStore,
                                  ResendOption resendOption,
                                  GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout,
                                  long resendTimeout,
                                  boolean skipGapsOnFullQueue) {
        this(streamId, partition, handler, keyStore, resendOption, groupKeyRequestFunction, propagationTimeout,
                resendTimeout, skipGapsOnFullQueue, null);
    }

    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  GroupKeyStore keyStore,
                                  ResendOption resendOption,
                                  GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, keyStore, resendOption, groupKeyRequestFunction,
                Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT,
                Subscription.DEFAULT_SKIP_GAPS_ON_FULL_QUEUE);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, GroupKeyStore keyStore, ResendOption resendOption) {
        this(streamId, partition, handler, keyStore, resendOption, null);
    }

    @Override
    public void onNewKeysAdded(Address publisherId, Collection<GroupKey> groupKeys) {
        super.onNewKeysAdded(publisherId, groupKeys);
        if (resendDone && encryptedMsgsQueues.isEmpty()) { // the messages in the queue were the last ones to handle
            this.handler.done(this);
        }
    }

    @Override
    public boolean isResending() {
        return true;
    }

    @Override
    public void setResending(boolean resending) {

    }

    @Override
    public boolean hasResendOptions() {
        return true;
    }

    @Override
    public ResendOption getResendOption() {
        return resendOption;
    }

    @Override
    public void startResend() {

    }

    @Override
    public void endResend() throws GapDetectedException {
        // Don't call the done handler until all encrypted messages have been handled (see onNewKeys)
        if (encryptedMsgsQueues.isEmpty()) {
            this.handler.done(this);
        } else { // received all historical messages but not yet the keys to decrypt them
            resendDone = true;
        }

    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        if (onRealTimeMsg != null) {
            onRealTimeMsg.accept(msg);
        }
    }

    @Override
    public Logger getLogger() {
        return log;
    }
}
