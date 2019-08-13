package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.exceptions.InvalidGroupKeyResponseException;
import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RealTimeSubscription extends BasicSubscription {
    private boolean resending = false;
    private final Map<String, SecretKey> groupKeys = new HashMap<>(); // publisherId --> groupKey
    private HashSet<String> alreadyFailedToDecrypt = new HashSet<>();
    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, GroupKey> groupKeys,
                                GroupKeyRequestFunction groupKeyRequestFunction, long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, groupKeyRequestFunction, propagationTimeout, resendTimeout);
        if (groupKeys != null) {
            for (String publisherId: groupKeys.keySet()) {
                String groupKeyHex = groupKeys.get(publisherId).getGroupKeyHex();
                this.groupKeys.put(publisherId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
            }
        }
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, GroupKey> groupKeys,
                                GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, groupKeys, groupKeyRequestFunction, Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, GroupKey> groupKeys) {
        this(streamId, partition, handler, groupKeys, null);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler) {
        this(streamId, partition, handler, null);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<GroupKey> groupKeys) {
        if (groupKeys.size() != 1) {
            throw new InvalidGroupKeyResponseException("Received "+groupKeys.size()+ " group keys for a real time subscription. Expected one.");
        }
        this.groupKeys.put(publisherId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeys.get(0).getGroupKeyHex()), "AES"));
        // handle real time messages received while waiting for the group key
        waitingForGroupKey.remove(publisherId);
        while (!encryptedMsgsQueue.isEmpty()) {
            handleInOrder(encryptedMsgsQueue.poll());
        }
    }

    @Override
    public boolean decryptOrRequestGroupKey(StreamMessage msg) {
        try {
            SecretKey newGroupKey = EncryptionUtil.decryptStreamMessage(msg, groupKeys.get(msg.getPublisherId()));
            alreadyFailedToDecrypt.remove(msg.getPublisherId());
            if (newGroupKey != null) {
                groupKeys.put(msg.getPublisherId(), newGroupKey);
            }
            return true;
        } catch (UnableToDecryptException e) {
            if (alreadyFailedToDecrypt.contains(msg.getPublisherId())) {
                // even after receiving the latest group key, we still cannot decrypt
                throw e;
            }
            groupKeyRequestFunction.apply(msg.getPublisherId(), null, null);
            // we will queue real time messages while waiting for the group key (including this one
            // since it could not be decrypted)
            waitingForGroupKey.add(msg.getPublisherId());
            encryptedMsgsQueue.offer(msg);
            alreadyFailedToDecrypt.add(msg.getPublisherId());
            return false;
        }

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
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        orderingUtil.add(msg);
    }
}
