package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnableToSetKeysException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.ResendRangeOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.DecryptionKeySequence;
import com.streamr.client.utils.UnencryptedGroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HistoricalSubscription extends BasicSubscription {

    private static final Logger log = LoggerFactory.getLogger(HistoricalSubscription.class);

    private final ResendOption resendOption;
    private boolean resendDone = false;
    private final Consumer<StreamMessage> onRealTimeMsg;
    private class KeyStorage {
        private final HashMap<String, DecryptionKeySequence> keySequences = new HashMap<>();
        public boolean containsKey(String publisherId) {
            return keySequences.containsKey(publisherId.toLowerCase());
        }
        public void put(String publisherId, DecryptionKeySequence keySequence) {
            keySequences.put(publisherId.toLowerCase(), keySequence);
        }
        public DecryptionKeySequence get(String publisherId) {
            return keySequences.get(publisherId.toLowerCase());
        }
    }
    private final KeyStorage keySequences = new KeyStorage();
    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys,
                                  GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout,
                                  long resendTimeout,
                                  boolean skipGapsOnFullQueue,
                                  Consumer<StreamMessage> onRealTimeMsg) {
        super(streamId, partition, handler, groupKeyRequestFunction, propagationTimeout, resendTimeout,
                skipGapsOnFullQueue);
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

    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys,
                                  GroupKeyRequestFunction groupKeyRequestFunction,
                                  long propagationTimeout,
                                  long resendTimeout,
                                  boolean skipGapsOnFullQueue) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction, propagationTimeout,
                resendTimeout, skipGapsOnFullQueue, null);
    }

    public HistoricalSubscription(String streamId,
                                  int partition,
                                  MessageHandler handler,
                                  ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys,
                                  GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, resendOption, groupKeys, groupKeyRequestFunction,
                Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT,
                Subscription.DEFAULT_SKIP_GAPS_ON_FULL_QUEUE);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, UnencryptedGroupKey> groupKeys) {
        this(streamId, partition, handler, resendOption, groupKeys, null);
    }

    public HistoricalSubscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this(streamId, partition, handler, resendOption, null);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<UnencryptedGroupKey> groupKeys) throws UnableToSetKeysException {
        if (keySequences.containsKey(publisherId)) {
            throw new UnableToSetKeysException("Received historical keys for publisher " + publisherId + " for a second time.");
        }
        keySequences.put(publisherId, new DecryptionKeySequence(groupKeys));
        // handle the historical messages received while we were waiting for the historical group keys
        handleInOrderQueue(publisherId);
        if (resendDone && encryptedMsgsQueues.isEmpty()) { // the messages in the queue were the last ones to handle
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
            log.debug("Failed to decrypt {}, requesting group key and queuing message", msg.getMessageRef());
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
