package com.streamr.client;

import com.streamr.client.exceptions.UnableToDecryptException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.options.ResendFromOption;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.protocol.message_layer.StreamMessageV31;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.HttpUtils;
import com.streamr.client.utils.StreamPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Subscription {
    private static final Logger log = LogManager.getLogger();

    private final String id;
    private final StreamPartition streamPartition;
    private final MessageHandler handler;
    private ResendOption resendOption;
    private final Map<String, SecretKey> groupKeys = new HashMap<>(); // publisherId --> groupKey

    private final HashMap<String, MessageRef> lastReceivedMsgRef = new HashMap<>();
    private boolean resending = false;
    private final ArrayDeque<StreamMessage> queue = new ArrayDeque<>();
    private final HashMap<String, GapDetectedException> gapDetectedExceptions = new HashMap<>();

    private State state;

    enum State {
        SUBSCRIBING, SUBSCRIBED, UNSUBSCRIBING, UNSUBSCRIBED
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption, Map<String, String> groupKeysHex) {
        this.id = UUID.randomUUID().toString();
        this.streamPartition = new StreamPartition(streamId, partition);
        this.handler = handler;
        this.resendOption = resendOption;
        for (String publisherId: groupKeysHex.keySet()) {
            String groupKeyHex = groupKeysHex.get(publisherId);
            groupKeys.put(publisherId, new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES"));
        }
    }

    public Subscription(String streamId, int partition, MessageHandler handler, ResendOption resendOption) {
        this(streamId, partition, handler, resendOption, new HashMap<>());
    }

    public Subscription(String streamId, int partition, MessageHandler handler) {
        this(streamId, partition, handler, null, new HashMap<>());
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

    public boolean isResending() {
        return resending;
    }

    public void setResending(boolean resending) {
        this.resending = resending;
    }

    public void handleMessage(StreamMessage msg) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        handleMessage(msg, false);
    }

    public void handleMessage(StreamMessage msg, boolean isResend) throws GapDetectedException, UnsupportedMessageException, UnableToDecryptException {
        String key = msg.getPublisherId() + msg.getMsgChainId();
        if (resending && !isResend) {
            queue.add(msg);
        } else if (checkForGap(msg.getPreviousMessageRef(), key) && !resending) {
            queue.add(msg);
            MessageRef last = lastReceivedMsgRef.get(key);
            MessageRef from = new MessageRef(last.getTimestamp(), last.getSequenceNumber() + 1);
            MessageRef to = msg.getPreviousMessageRef();
            GapDetectedException e = new GapDetectedException(msg.getStreamId(), msg.getStreamPartition(), from, to, msg.getPublisherId(), msg.getMsgChainId());
            gapDetectedExceptions.put(key, e);
            throw e;
        } else {
            // The potential gap is filled if we get to this point, so we can clear the exception.
            gapDetectedExceptions.remove(key);
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
                StreamMessage decrypted = getDecryptedMessage((StreamMessageV31)msg);
                handler.onMessage(this, decrypted);
            }
        }
    }

    private void handleQueue() throws GapDetectedException {
        while(!queue.isEmpty()) {
            StreamMessage msg = queue.poll();
            handleMessage(msg);
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

    public void startResend() {
        resending = true;
    }

    public void endResend() throws GapDetectedException {
        resending = false;
        handleQueue();
    }

    public GapDetectedException getGapDetectedException(String publisherId, String msgChainId) {
        return gapDetectedExceptions.get(publisherId + msgChainId);
    }

    public boolean isSubscribed() {
        return state.equals(State.SUBSCRIBED);
    }

    private boolean checkForGap(MessageRef prev, String key) {
        if (prev == null) {
            return false;
        }
        MessageRef last = lastReceivedMsgRef.get(key);
        return last != null && prev.compareTo(last) > 0;
    }

    void resendDone() {
        if (this.resending) {
            log.error("Resending should be done");
        }

        this.handler.done(this);
    }

    //TODO: This method returns a new StreamMessage because MessageHandler takes a StreamMessage as argument.
    // Shouldn't this method return only the content (Map<String, Object>) and the MessageHandler only take the
    // content as argument?
    private StreamMessage getDecryptedMessage(StreamMessageV31 msg) throws UnsupportedMessageException, UnableToDecryptException {
        if (msg.getContentType() == StreamMessage.ContentType.CONTENT_TYPE_JSON) {
            if (msg.getEncryptionType() == StreamMessage.EncryptionType.NONE) {
                return msg;
            } else if (msg.getEncryptionType() == StreamMessage.EncryptionType.AES) {
                try {
                    String plaintext = new String(EncryptionUtil.decrypt(msg.getSerializedContent(), groupKeys.get(msg.getPublisherId())), StandardCharsets.UTF_8);
                    Map<String, Object> content = HttpUtils.mapAdapter.fromJson(plaintext);
                    return new StreamMessageV31(msg.getMessageID(), msg.getPreviousMessageRef(),
                            msg.getContentType(), StreamMessage.EncryptionType.NONE, content, msg.getSignatureType(), msg.getSignature());
                } catch (Exception e) {
                    throw new UnableToDecryptException(msg.getSerializedContent());
                }

            } else if (msg.getEncryptionType() == StreamMessage.EncryptionType.NEW_KEY_AND_AES) {
                try {
                    byte[] plaintext = EncryptionUtil.decrypt(msg.getSerializedContent(), groupKeys.get(msg.getPublisherId()));
                    groupKeys.put(msg.getPublisherId(), new SecretKeySpec(Arrays.copyOfRange(plaintext, 0, 32), "AES"));
                    String serializedContent = new String(Arrays.copyOfRange(plaintext, 32, plaintext.length), StandardCharsets.UTF_8);
                    Map<String, Object> content = HttpUtils.mapAdapter.fromJson(serializedContent);
                    return new StreamMessageV31(msg.getMessageID(), msg.getPreviousMessageRef(),
                            msg.getContentType(), StreamMessage.EncryptionType.NONE, content, msg.getSignatureType(), msg.getSignature());
                } catch (Exception e) {
                    throw new UnableToDecryptException(msg.getSerializedContent());
                }
            }
            throw new UnsupportedMessageException("Unknown encryption type: "+msg.getEncryptionType());
        }
        //TODO: Handle other content types
        throw new UnsupportedMessageException("Unknown content type: "+msg.getContentType());
    }
}
