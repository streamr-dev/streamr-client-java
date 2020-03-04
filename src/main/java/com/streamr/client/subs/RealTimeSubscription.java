package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.*;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.OrderedMsgChain;
import com.streamr.client.utils.UnencryptedGroupKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RealTimeSubscription extends BasicSubscription {
    private boolean resending = false;
    private class KeyStorage {
        private final HashMap<String, SecretKey> groupKeys = new HashMap<>(); // publisherId --> groupKey
        public void put(String publisherId, SecretKey key) {
            groupKeys.put(publisherId.toLowerCase(), key);
        }
        public SecretKey get(String publisherId) {
            return groupKeys.get(publisherId.toLowerCase());
        }
    }
    private final KeyStorage groupKeys = new KeyStorage();
    private final HashSet<String> alreadyFailedToDecrypt = new HashSet<>();
    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, UnencryptedGroupKey> groupKeys,
                                GroupKeyRequestFunction groupKeyRequestFunction, long propagationTimeout, long resendTimeout) {
        super(streamId, partition, handler, groupKeyRequestFunction, propagationTimeout, resendTimeout);
        if (groupKeys != null) {
            for (String publisherId: groupKeys.keySet()) {
                this.groupKeys.put(publisherId, groupKeys.get(publisherId).getSecretKey());
            }
        }
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, UnencryptedGroupKey> groupKeys,
                                GroupKeyRequestFunction groupKeyRequestFunction) {
        this(streamId, partition, handler, groupKeys, groupKeyRequestFunction, Subscription.DEFAULT_PROPAGATION_TIMEOUT, Subscription.DEFAULT_RESEND_TIMEOUT);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler, Map<String, UnencryptedGroupKey> groupKeys) {
        this(streamId, partition, handler, groupKeys, null);
    }

    public RealTimeSubscription(String streamId, int partition, MessageHandler handler) {
        this(streamId, partition, handler, null);
    }

    @Override
    public void setGroupKeys(String publisherId, ArrayList<UnencryptedGroupKey> groupKeys) throws InvalidGroupKeyResponseException {
        if (groupKeys.size() != 1) {
            throw new InvalidGroupKeyResponseException("Received "+groupKeys.size()+ " group keys for a real time subscription. Expected one.");
        }
        this.groupKeys.put(publisherId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeys.get(0).getGroupKeyHex()), "AES"));
        // handle real time messages received while waiting for the group key
        handleInOrderQueue(publisherId);
    }

    @Override
    public boolean decryptOrRequestGroupKey(StreamMessage msg) throws UnableToDecryptException {
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
            // we will queue real time messages while waiting for the group key (including this one
            // since it could not be decrypted)
            requestGroupKeyAndQueueMessage(msg, null, null);
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
    }

    @Override
    public void handleRealTimeMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException {
        orderingUtil.add(msg);
    }

    public void setLastMessageRefs(ArrayList<OrderedMsgChain> chains) {
        orderingUtil.addChains(chains);
    }
}
