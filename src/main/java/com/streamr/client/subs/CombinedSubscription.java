package com.streamr.client.subs;

import com.streamr.client.MessageHandler;
import com.streamr.client.exceptions.*;
import com.streamr.client.options.ResendOption;
import com.streamr.client.protocol.common.UnsupportedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.*;
import java.util.ArrayDeque;
import java.util.Collection;

public class CombinedSubscription extends Subscription {

  private BasicSubscription currentSub;
  private final ArrayDeque<StreamMessage> queuedRealtimeMessages = new ArrayDeque<>();

  public CombinedSubscription(
      String streamId,
      int partition,
      MessageHandler handler,
      GroupKeyStore keyStore,
      KeyExchangeUtil keyExchangeUtil,
      ResendOption resendOption,
      BasicSubscription.GroupKeyRequestFunction groupKeyRequestFunction,
      long propagationTimeout,
      long resendTimeout,
      boolean skipGapsOnFullQueue) {

    super(
        streamId,
        partition,
        handler,
        keyStore,
        keyExchangeUtil,
        propagationTimeout,
        resendTimeout,
        skipGapsOnFullQueue);

    MessageHandler wrapperHandler =
        new MessageHandler() {
          @Override
          public void onMessage(Subscription sub, StreamMessage message) {
            handler.onMessage(sub, message);
          }

          @Override
          public void done(Subscription s) {
            handler.done(s);

            log.debug(
                "HistoricalSubscription for stream {} is done. Switching to RealtimeSubscription.",
                streamId);

            // once the initial resend is done, switch to real time
            RealTimeSubscription realTime =
                new RealTimeSubscription(
                    streamId,
                    partition,
                    handler,
                    keyStore,
                    keyExchangeUtil,
                    groupKeyRequestFunction,
                    propagationTimeout,
                    resendTimeout,
                    skipGapsOnFullQueue);

            realTime.setGapHandler(currentSub.getGapHandler());
            // set the last received references to the last references of the resent messages
            realTime.setLastMessageRefs(currentSub.getChains());
            // handle the real time messages received during the initial resend
            while (!queuedRealtimeMessages.isEmpty()) {
              StreamMessage msg = queuedRealtimeMessages.poll();
              realTime.handleRealTimeMessage(msg);
            }
            currentSub = realTime;
          }

          @Override
          public void onUnableToDecrypt(UnableToDecryptException e) {
            handler.onUnableToDecrypt(e);
          }
        };
    // starts to request the initial resend
    currentSub =
        new HistoricalSubscription(
            streamId,
            partition,
            wrapperHandler,
            keyStore,
            keyExchangeUtil,
            resendOption,
            groupKeyRequestFunction,
            propagationTimeout,
            resendTimeout,
            skipGapsOnFullQueue,
            queuedRealtimeMessages::push);
  }

  @Override
  public void setGapHandler(OrderedMsgChain.GapHandlerFunction gapHandler) {
    currentSub.setGapHandler(gapHandler);
  }

  @Override
  public void onNewKeysAdded(Address publisherId, Collection<GroupKey> groupKeys) {
    currentSub.onNewKeysAdded(publisherId, groupKeys);
  }

  @Override
  public boolean isResending() {
    return currentSub.isResending();
  }

  @Override
  public void setResending(boolean resending) {
    currentSub.setResending(resending);
  }

  @Override
  public boolean hasResendOptions() {
    return currentSub.hasResendOptions();
  }

  @Override
  public ResendOption getResendOption() {
    return currentSub.getResendOption();
  }

  @Override
  public void startResend() {
    currentSub.startResend();
  }

  @Override
  public void endResend() throws GapDetectedException {
    currentSub.endResend();
  }

  @Override
  public void handleRealTimeMessage(StreamMessage msg)
      throws GapDetectedException, UnsupportedMessageException {
    currentSub.handleRealTimeMessage(msg);
  }

  @Override
  public void handleResentMessage(StreamMessage msg)
      throws GapDetectedException, UnsupportedMessageException {
    currentSub.handleResentMessage(msg);
  }

  @Override
  public void clear() {
    currentSub.clear();
  }
}
