package com.streamr.client.utils;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.utils.Address;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderedMsgChain {
  private static final Logger log = LoggerFactory.getLogger(OrderedMsgChain.class);
  private static final int MAX_GAP_REQUESTS = 10;

  static final int MAX_QUEUE_SIZE = 10000;

  private final Address publisherId;
  private final String msgChainId;
  private final Consumer<StreamMessage> inOrderHandler;
  private final GapHandlerFunction gapHandler;
  private final Function<GapFillFailedException, Void> gapFillFailedHandler;
  private final long propagationTimeout;
  private final long resendTimeout;
  private final PriorityQueue<StreamMessage> queue;
  private MessageRef lastReceived = null;
  private Timer gap = null;
  private int gapRequestCount = 0;
  private final boolean skipGapsOnFullQueue;
  private final GapFillFailedException gapException = null;

  public OrderedMsgChain(
      Address publisherId,
      String msgChainId,
      Consumer<StreamMessage> inOrderHandler,
      GapHandlerFunction gapHandler,
      Function<GapFillFailedException, Void> gapFillFailedHandler,
      long propagationTimeout,
      long resendTimeout,
      boolean skipGapsOnFullQueue) {
    this.publisherId = publisherId;
    this.msgChainId = msgChainId;
    this.inOrderHandler = inOrderHandler;
    this.gapHandler = gapHandler;
    this.gapFillFailedHandler = gapFillFailedHandler;
    this.propagationTimeout = propagationTimeout;
    this.resendTimeout = resendTimeout;
    this.skipGapsOnFullQueue = skipGapsOnFullQueue;
    queue =
        new PriorityQueue<>(
            new Comparator<StreamMessage>() {
              @Override
              public int compare(StreamMessage o1, StreamMessage o2) {
                return o1.getMessageRef().compareTo(o2.getMessageRef());
              }
            });
  }

  public OrderedMsgChain(
      Address publisherId,
      String msgChainId,
      Consumer<StreamMessage> inOrderHandler,
      GapHandlerFunction gapHandler,
      long propagationTimeout,
      long resendTimeout,
      boolean skipGapsOnFullQueue) {
    this(
        publisherId,
        msgChainId,
        inOrderHandler,
        gapHandler,
        (GapFillFailedException e) -> {
          throw e;
        },
        propagationTimeout,
        resendTimeout,
        skipGapsOnFullQueue);
  }

  public synchronized void add(StreamMessage unorderedMsg) {
    MessageRef ref = unorderedMsg.getMessageRef();
    if (lastReceived != null && ref.compareTo(lastReceived) <= 0) {
      log.debug(
          "Already received message: "
              + ref
              + ", lastReceivedMsgRef: "
              + lastReceived
              + ". Ignoring message.");
      return;
    }
    if (isNextMessage(unorderedMsg)) {
      process(unorderedMsg);
      checkQueue();
    } else {
      if (gap == null) {
        scheduleGap();
      }
      // Prevent memory exhaustion under unusual conditions by limiting the queue size
      if (queue.size() < MAX_QUEUE_SIZE) {
        queue.offer(unorderedMsg);
      } else {

        // Form diagnosis string
        List<StreamMessage> queueAsArrayList = new ArrayList<>(queue);
        String diagnosisString =
            String.format(
                "Queue for %s::%d was (%s, ..., %s) and new message is %s",
                unorderedMsg.getStreamId(),
                unorderedMsg.getStreamPartition(),
                queueAsArrayList.get(0).getMessageRef(),
                queueAsArrayList.get(queueAsArrayList.size() - 1).getMessageRef(),
                unorderedMsg.getMessageRef());

        System.out.println(skipGapsOnFullQueue);
        if (skipGapsOnFullQueue) {
          log.warn("Queue is full. Emptying and processing new message. " + diagnosisString);
          clearGap();
          queue.clear();
          process(unorderedMsg);
        } else {
          throw new IllegalStateException("Queue is full! Message." + diagnosisString);
        }
      }
    }
  }

  synchronized void clearGap() {
    if (gap != null) {
      gap.cancel();
      gap.purge();
      gap = null;
      if (gapException != null) { // TODO: gapException is always null
        throw gapException;
      }
    }
  }

  public synchronized boolean hasGap() {
    return gap != null;
  }

  public Address getPublisherId() {
    return publisherId;
  }

  public String getMsgChainId() {
    return msgChainId;
  }

  public synchronized void setLastReceived(MessageRef lastReceived) {
    this.lastReceived = lastReceived;
  }

  public synchronized MessageRef getLastReceived() {
    return lastReceived;
  }

  private boolean isNextMessage(StreamMessage msg) {
    boolean isFirstMessage = lastReceived == null;
    return isFirstMessage
        // is chained and next
        || (msg.getPreviousMessageRef() != null
            && msg.getPreviousMessageRef().compareTo(lastReceived) == 0)
        // is unchained and newer
        || (msg.getPreviousMessageRef() == null && msg.getMessageRef().compareTo(lastReceived) > 0);
  }

  private void checkQueue() {
    while (!queue.isEmpty()) {
      StreamMessage msg = queue.peek();
      if (msg != null && isNextMessage(msg)) {
        queue.poll();

        // If the next message is found in the queue, any gap must have been filled, so clear the
        // timer
        clearGap();
        process(msg);
      } else if (msg != null
          && lastReceived != null
          && msg.getMessageRef().compareTo(lastReceived) <= 0) {
        // If there are old (already received) messages in the queue for any reason, remove them
        queue.poll();
      } else {
        // Nothing further can be processed from the queue
        break;
      }
    }
  }

  private void process(StreamMessage msg) {
    lastReceived = msg.getMessageRef();
    inOrderHandler.accept(msg);
  }

  private void scheduleGap() {
    gapRequestCount = 0;
    gap = new Timer(true);
    TimerTask task =
        new TimerTask() {
          @Override
          public void run() {
            synchronized (OrderedMsgChain.this) {
              // Make sure nothing further can be processed from the queue
              checkQueue();

              // Make sure a gapfill is still scheduled and there is a queued message
              if (gap == null || queue.isEmpty()) {
                return;
              }

              MessageRef from =
                  new MessageRef(lastReceived.getTimestamp(), lastReceived.getSequenceNumber() + 1);
              MessageRef to = queue.peek().getPreviousMessageRef();

              // Sanity check
              if (from.compareTo(to) > 0) {
                throw new IllegalStateException(
                    String.format("From (%s) is after to (%s)!", from.toString(), to.toString()));
              }

              // Request gapfill or fail if max requests reached
              if (gapRequestCount < MAX_GAP_REQUESTS) {
                gapRequestCount++;
                if (gapHandler != null) {
                  gapHandler.apply(from, to, publisherId, msgChainId);
                } else {
                  log.error(
                      String.format(
                          "Failed to request gapfill because the gapHandler is null. streamId %s, streamPartition %d, publisherId %s, msgChainId %s.",
                          queue.peek().getStreamId(),
                          queue.peek().getStreamPartition(),
                          publisherId,
                          msgChainId));
                }
              } else {
                try {
                  if (gapFillFailedHandler != null) {
                    gapFillFailedHandler.apply(
                        new GapFillFailedException(
                            from, to, publisherId, msgChainId, MAX_GAP_REQUESTS));
                  } else {
                    log.error(
                        String.format(
                            "Failed to report failed gapfill because gapFillFailedHandler is null. streamId %s, streamPartition %d, publisherId %s, msgChainId %s.",
                            queue.peek().getStreamId(),
                            queue.peek().getStreamPartition(),
                            publisherId,
                            msgChainId));
                  }
                } finally {
                  clearGap();

                  // TODO: make it configurable how to handle this error situation.
                  // Currently unrecoverable gaps are just ignored, and processing continues from
                  // the next
                  // message after the gap.
                  log.warn(
                      "Unable to fill gap: Max retries reached! Ignoring the error and continuing from the first processable message: "
                          + queue.peek().getMessageRef());
                  lastReceived = queue.peek().getPreviousMessageRef();
                  checkQueue();
                }
              }
            }
          }
        };
    gap.schedule(task, propagationTimeout, resendTimeout);
  }

  // Used in unit test only
  boolean isQueueFull() {
    return this.queue.size() == MAX_QUEUE_SIZE;
  }

  @FunctionalInterface
  public interface GapHandlerFunction {
    void apply(MessageRef from, MessageRef to, Address publisherId, String msgChainId);
  }
}
