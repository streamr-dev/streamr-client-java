package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.common.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.stream.GroupKey;
import com.streamr.client.stream.GroupKeyStore;
import com.streamr.client.stream.KeyExchangeUtil;
import com.streamr.client.utils.*;
import java.util.Collection;
import java.util.UUID;

public abstract class Subscription {
  public static final long DEFAULT_PROPAGATION_TIMEOUT = 5000L;
  public static final long DEFAULT_RESEND_TIMEOUT = 5000L;
  public static final boolean DEFAULT_SKIP_GAPS_ON_FULL_QUEUE = true;

  protected final String streamId;
  protected final int partition;
  private final String id;
  protected final MessageHandler handler;
  protected final GroupKeyStore keyStore;
  protected final KeyExchangeUtil keyExchangeUtil;
  protected final long propagationTimeout;
  protected final long resendTimeout;
  protected final boolean skipGapsOnFullQueue;

  private State state;

  public enum State {
    SUBSCRIBING,
    SUBSCRIBED,
    UNSUBSCRIBING,
    UNSUBSCRIBED
  }

  public Subscription(
      String streamId,
      int partition,
      MessageHandler handler,
      GroupKeyStore keyStore,
      KeyExchangeUtil keyExchangeUtil,
      long propagationTimeout,
      long resendTimeout,
      boolean skipGapsOnFullQueue) {
    this.id = UUID.randomUUID().toString();
    this.streamId = streamId;
    this.partition = partition;
    this.handler = handler;
    this.keyStore = keyStore;
    this.keyExchangeUtil = keyExchangeUtil;
    this.propagationTimeout = propagationTimeout;
    this.resendTimeout = resendTimeout;
    this.skipGapsOnFullQueue = skipGapsOnFullQueue;
  }

  public String getId() {
    return id;
  }

  public String getStreamId() {
    return streamId;
  }

  public int getPartition() {
    return partition;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public boolean isSubscribed() {
    return state.equals(State.SUBSCRIBED);
  }

  public abstract boolean isResending();

  public abstract void setResending(boolean resending);

  public abstract boolean hasResendOptions();

  public abstract ResendOption getResendOption();

  public abstract void startResend();

  public abstract void endResend() throws GapDetectedException;

  public abstract void handleRealTimeMessage(StreamMessage msg)
      throws GapDetectedException, UnsupportedMessageException;

  public abstract void handleResentMessage(StreamMessage msg)
      throws GapDetectedException, UnsupportedMessageException;

  public abstract void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler);

  /**
   * Method to be called whenever the client has new encryption keys available in the GroupKeyStore.
   * The Subscriptions should check the messages in its encryption queue and attempt to decrypt
   * them.
   */
  public abstract void onNewKeysAdded(Address publisherId, Collection<GroupKey> groupKeys);

  public abstract void clear();
}
