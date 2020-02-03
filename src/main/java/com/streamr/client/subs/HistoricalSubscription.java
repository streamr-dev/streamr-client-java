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
import com.streamr.client.utils.UnencryptedGroupKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class HistoricalSubscription extends BasicSubscription {
    private final ResendOption resendOption;
    private boolean resendDone = false;
    private final Consumer<StreamMessage> onRealTimeMsg;
    private final HashMap<String, DecryptionKeySequence> keySequences = new HashMap<>();
    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout, long resendTimeout, Consumer<StreamMessage> onRealTimeMsg) {
        super(streamId, partition, handler, groupKeyRequestFunction, propagationTimeout, resendTimeout);
        this.resendOption = resendOption;
        this.onRealTimeMsg = onRealTimeMsg;
        if (groupKeys != null) {
            for (String publisherId: groupKeys.keySet()) {
                ArrayList<UnencryptedGroupKey> keys = new ArrayList<>();
                keys.add(groupKeys.get(publisherId));
                keySequences.put(publisherId, new DecryptionKeySequence(keys));
            }
        }
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout, long resendTimeout) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction, propagationTimeout, resendTimeout, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys, GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction,
                Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, UnencryptedGroupKey> groupKeys) {
        this(streamId, partition, handler, resendOption, groupKeys, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this(streamId, partition, handler, resendOption, null);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<UnencryptedGroupKey> groupKeys) throws InvalidGroupKeyResponseException {
        if (keySequences.containsKey(publisherId)) {
            throw new InvalidGroupKeyResponseException("Received historical keys for publisher " + publisherId + " for a second time.");
        }
        keySequences.put(publisherId, new DecryptionKeySequence(groupKeys));
        // handle the historical messages received while we were waiting for the historical group keys
        handleInOrderQueue(publisherId);
        if (resendDone && encryptedMsgsQueue.isEmpty()) { // the messages in the queue were the last ones to handle
            this.handler.done(this);
        }
    }

    @Override
    public boolean decryptOrRequestGroupKey(StreamMessage msg) throws UnableToDecryptException {
        if (msg.getEncryptionType() != StreamMessage.EncryptionType.AES && msg.getEncryptionType() != StreamMessage.EncryptionType.NEW_KEY_AND_AES) {
            return true;
        }
        // If we don't have the historical keys, we request them.
        if (!keySequences.containsKey(msg.getPublisherId())) {
            Date start = msg.getTimestampAsDate();
            Date end = resendOption instanceof ResendRangeOption ? ((ResendRangeOption) resendOption).getTo().getTimestampAsDate() : new Date();
            requestGroupKeyAndQueueMessage(msg, start, end);
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
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        if (onRealTimeMsg != null) {
            onRealTimeMsg.accept(msg);
        }
    }
}
