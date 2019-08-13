package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.InvalidGroupKeyResponseException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.ResendRangeOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.DecryptionKeySequence;
import com.streamr.client.utils.GroupKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class HistoricalSubscription extends BasicSubscription {
    private ResendOption resendOption;
    private boolean resendDone = false;
    private Function<StreamMessage, Void> onRealTimeMsg;
    private HashMap<String, DecryptionKeySequence> keySequences = new HashMap<>();
    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, GroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout, long resendTimeout, Function<StreamMessage, Void> onRealTimeMsg) {
        super(streamId, partition, handler, groupKeys, groupKeyRequestFunction, propagationTimeout, resendTimeout);
        this.resendOption = resendOption;
        this.onRealTimeMsg = onRealTimeMsg;
        if (groupKeys != null) {
            for (String publisherId: groupKeys.keySet()) {
                ArrayList<GroupKey> keys = new ArrayList<>();
                keys.add(groupKeys.get(publisherId));
                keySequences.put(publisherId, new DecryptionKeySequence(keys));
            }
        }
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, GroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout, long resendTimeout) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction, propagationTimeout, resendTimeout, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, GroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction,
                Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, GroupKey> groupKeys) {
        this(streamId, partition, handler, resendOption, groupKeys, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this(streamId, partition, handler, resendOption, null);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<GroupKey> groupKeys) {
        if (keySequences.containsKey(publisherId)) {
            throw new InvalidGroupKeyResponseException("Received historical keys for publisher " + publisherId + " for a second time.");
        }

        keySequences.put(publisherId, new DecryptionKeySequence(groupKeys));
        waitingForGroupKey.remove(publisherId);
        while (!encryptedMsgsQueue.isEmpty()) {
            handleInOrder(encryptedMsgsQueue.poll());
        }
        if (resendDone) { // the messages in the queue were the last ones to handle
            this.handler.done(this);
        }
    }

    @Override
    public boolean decryptOrRequestGroupKey(StreamMessage msg) {
        if (msg.getEncryptionType() != StreamMessage.EncryptionType.AES && msg.getEncryptionType() != StreamMessage.EncryptionType.NEW_KEY_AND_AES) {
            return true;
        }
        // If we don't have the historical keys, we request them.
        if (!keySequences.containsKey(msg.getPublisherId())) {
            Date start = msg.getTimestampAsDate();
            Date end = resendOption instanceof ResendRangeOption ? ((ResendRangeOption) resendOption).getTo().getTimestampAsDate() : new Date();
            groupKeyRequestFunction.apply(msg.getPublisherId(), start, end);
            waitingForGroupKey.add(msg.getPublisherId());
            encryptedMsgsQueue.offer(msg);
            return false;
        }
        // Could fail to decrypt if the received historical keys are wrong. We don't request them a second time.
        keySequences.get(msg.getPublisherId()).tryToDecryptResent(msg);
        return true;
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
        if (encryptedMsgsQueue.isEmpty()) {
            this.handler.done(this);
        } else { // received all historical messages but not yet the keys to decrypt them
            resendDone = true;
        }

    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        if (onRealTimeMsg != null) {
            onRealTimeMsg.apply(msg);
        }
    }
}
