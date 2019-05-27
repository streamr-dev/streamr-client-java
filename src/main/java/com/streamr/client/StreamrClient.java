package com.streamr.client;

import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.GapDetectedException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.*;
import com.streamr.client.utils.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.streamr.client.exceptions.ConnectionTimeoutException;
import com.streamr.client.exceptions.PartitionNotSpecifiedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.*;

/**
 * Extends the StreamrRESTClient with methods for using the websocket protocol.
 */
public class StreamrClient extends StreamrRESTClient {

    private static final Logger log = LogManager.getLogger();

    // Underlying websocket implementation
    private final WebSocketClient websocket;

    protected final Subscriptions subs = new Subscriptions();

    public enum State {
        Connecting, Connected, Disconnecting, Disconnected
    }

    private State state = State.Disconnected;
    private Exception errorWhileConnecting = null;

    private String publisherId = null;
    private final MessageCreationUtil msgCreationUtil;
    private final SubscribedStreamsUtil subscribedStreamsUtil;

    private final HashMap<String, OneTimeResend> secondResends = new HashMap<>();

    public StreamrClient(StreamrClientOptions options) {
        super(options);

        subscribedStreamsUtil = new SubscribedStreamsUtil(id -> {
            try {
                return getStream(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, id -> {
            try {
                return getPublishers(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, options.getSigningOptions().getVerifySignatures());

        if (options.getAuthenticationMethod() instanceof ApiKeyAuthenticationMethod) {
            try {
                publisherId = DigestUtils.sha256Hex(getUserInfo().getUsername());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (options.getAuthenticationMethod() instanceof EthereumAuthenticationMethod) {
            publisherId = ((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAddress();
        }
        SigningUtil signingUtil = null;
        if (options.getPublishSignedMsgs()) {
            signingUtil = new SigningUtil(((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAccount());
        }
        msgCreationUtil = new MessageCreationUtil(publisherId, signingUtil, options.getEncryptionOptions().getPublisherGroupKeys());

        try {
            this.websocket = new WebSocketClient(new URI(options.getWebsocketApiUrl())) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Connection established");
                    state = State.Connected;
                    StreamrClient.this.onOpen();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Connection closed! Code: " + code + ", Reason: " + reason);
                    state = State.Disconnected;
                    StreamrClient.this.onClose();
                }

                @Override
                public void onError(Exception ex) {
                    log.error(ex);

                    if (state == State.Connecting) {
                        errorWhileConnecting = ex;
                    }

                    StreamrClient.this.onError(ex);
                }

                @Override
                public void send(String text) throws NotYetConnectedException {
                    log.info(">> " + text);
                    super.send(text);
                }
            };
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Connecting and disconnecting
     */

    public void onOpen() {}
    public void onClose() {}
    public void onError(Exception ex) {}

    public WebSocketClient getWebsocket() {
        return websocket;
    }

    /**
     * Connects the websocket. Blocks until connected, or throws if the connection times out.
     */
    public void connect() throws ConnectionTimeoutException {
        log.info("Connecting to " + options.getWebsocketApiUrl() + "...");
        state = State.Connecting;

        websocket.connect();
        waitForState(State.Connected);

        if (errorWhileConnecting != null) {
            Exception ex = errorWhileConnecting;
            errorWhileConnecting = null;
            throw new RuntimeException(ex);
        } else if (state != State.Connected) {
            throw new ConnectionTimeoutException(options.getWebsocketApiUrl());
        }
    }

    /**
     * Disconnects the websocket. Blocks until disconnected, or throws if the operation times out.
     */
    public void disconnect() throws ConnectionTimeoutException {
        log.info("Disconnecting...");
        state = State.Disconnecting;

        websocket.close();
        waitForState(State.Disconnected);

        if (errorWhileConnecting != null) {
            throw new RuntimeException(errorWhileConnecting);
        } else if (state != State.Disconnected) {
            throw new ConnectionTimeoutException(options.getWebsocketApiUrl());
        }
    }

    private void waitForState(State target) {
        long iterations = options.getConnectionTimeoutMillis() / 100;
        while (errorWhileConnecting == null && state != target && iterations > 0) {
            try {
                Thread.sleep(100);
                iterations--;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public State getState() {
        return state;
    }

    public String getPublisherId() {
        return publisherId;
    }

    /*
     * Message handling
     */

    protected void handleMessage(String rawMessageAsString) {
        try {
            log.info("<< " + rawMessageAsString);

            // Handle different message types
            ControlMessage message = ControlMessage.fromJson(rawMessageAsString);
            if (message != null) {
                try {
                    if (message.getType() == BroadcastMessage.TYPE) {
                        BroadcastMessage msg = (BroadcastMessage) message;
                        handleMessage(msg.getStreamMessage(), false);
                    } else if (message.getType() == UnicastMessage.TYPE) {
                        UnicastMessage msg = (UnicastMessage) message;
                        handleMessage(msg.getStreamMessage(), true);
                    } else if (message.getType() == SubscribeResponse.TYPE) {
                        handleSubcribeResponse((SubscribeResponse)message);
                    } else if (message.getType() == UnsubscribeResponse.TYPE) {
                        handleUnsubcribeResponse((UnsubscribeResponse)message);
                    } else if (message.getType() == ResendResponseResending.TYPE) {
                        handleResendResponseResending((ResendResponseResending)message);
                    } else if (message.getType() == ResendResponseNoResend.TYPE) {
                        handleResendResponseNoResend((ResendResponseNoResend)message);
                    } else if (message.getType() == ResendResponseResent.TYPE) {
                        handleResendResponseResent((ResendResponseResent)message);
                    }
                } catch (Exception e) {
                    log.error("Error handling message: " + message, e);
                }
            } else {
                log.error("Parsed message was null! Raw message: " + rawMessageAsString);
            }
        } catch (Exception e) {
            log.error("Error while handling message: " + rawMessageAsString, e);
        }
    }

    private void handleMessage(StreamMessage message, boolean isResend) throws SubscriptionNotFoundException {
        try {
            log.debug(message.getStreamId() + ": " + message.getContent().toString());
        } catch (IOException e) {
            throw new MalformedMessageException(message.toJson());
        }

        subscribedStreamsUtil.verifyStreamMessage(message);
        Subscription sub = subs.get(message.getStreamId(), message.getStreamPartition());

        // Only call the handler if we are in subscribed state (and not for example UNSUBSCRIBING)
        if (sub.isSubscribed()) {
            // we can clear the second resend upon reception of a message because gap filling will
            // take care of sending other resend requests if needed.
            OneTimeResend resend = secondResends.get(sub.getId());
            if (resend != null) {
                resend.interrupt();
                secondResends.remove(sub.getId());
            }
            try {
                sub.handleMessage(message, isResend);
            } catch (GapDetectedException e) {
                ResendRangeRequest req = new ResendRangeRequest(e.getStreamId(), e.getStreamPartition(),
                    sub.getId(), e.getFrom(), e.getTo(), e.getPublisherId(), e.getMsgChainId(), getSessionToken());
                sub.setResending(true);
                this.websocket.send(req.toJson());
                new PeriodicResend(websocket, options.getGapFillTimeout(), sub, e.getPublisherId(),
                    e.getMsgChainId(), getSessionToken()).start();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    /*
     * Publish
     */

    public void publish(Stream stream, Map<String, Object> payload) {
        publish(stream, payload, new Date(), null);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp) {
        publish(stream, payload, timestamp, null);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, String groupKeyHex) {
        if (!getState().equals(StreamrClient.State.Connected)) {
            connect();
        }
        if (groupKeyHex != null) {
            options.getEncryptionOptions().getPublisherGroupKeys().put(stream.getId(), groupKeyHex);
        }
        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, timestamp, null, groupKeyHex);
        PublishRequest req = new PublishRequest(streamMessage, getSessionToken());
        getWebsocket().send(req.toJson());
    }

    /*
     * Subscribe
     */

    public Subscription subscribe(Stream stream, MessageHandler handler) {
        Integer nbPartitions = stream.getPartitions();
        if (nbPartitions != null && nbPartitions > 1) {
            throw new PartitionNotSpecifiedException(stream.getId(), stream.getPartitions());
        }
        return subscribe(stream, 0, handler, null, null);
    }

    public Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
        return subscribe(stream, partition, handler, resendOption, null);
    }

    public Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, String> groupKeys) {
        if (!getState().equals(State.Connected)) {
            connect();
        }

        if (groupKeys != null) {
            Map<String, String> keysPerPublisher = options.getEncryptionOptions().getSubscriberGroupKeys().get(stream.getId());
            if (keysPerPublisher == null) {
                options.getEncryptionOptions().getSubscriberGroupKeys().put(stream.getId(), new HashMap<>(groupKeys));
            } else {
                keysPerPublisher.putAll(groupKeys);
            }
        }

        SubscribeRequest subscribeRequest = new SubscribeRequest(stream.getId(), partition, session.getSessionToken());
        Subscription sub = new Subscription(stream.getId(), partition, handler, resendOption, groupKeys);
        subs.add(sub);
        sub.setState(Subscription.State.SUBSCRIBING);
        this.websocket.send(subscribeRequest.toJson());
        return sub;
    }

    /*
     * Resend
     */

    public void resend(Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
        StreamrClient s = this;

        MessageHandler a = new MessageHandler() {
            StreamrClient sc = s;

            @Override
            public void onMessage(Subscription sub, StreamMessage message) {
                handler.onMessage(sub, message);
            }
            public void done(Subscription sub) {
                if (sc != null) {
                    sc.unsubscribe(sub);
                }

                handler.done(sub);
            }
        };

        subscribe(stream, partition, a, resendOption);
    }

    /*
     * Unsubscribe
     */

    public void unsubscribe(Subscription sub) {
        UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest(sub.getStreamId(), sub.getPartition());
        sub.setState(Subscription.State.UNSUBSCRIBING);
        sub.setResending(false);
        this.websocket.send(unsubscribeRequest.toJson());
    }

    private void handleSubcribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.SUBSCRIBED);
        if (sub.hasResendOptions()) {
            ResendOption resendOption = sub.getEffectiveResendOption();
            ControlMessage req = resendOption.toRequest(res.getStreamId(), res.getStreamPartition(), sub.getId(), this.getSessionToken());
            this.websocket.send(req.toJson());
            OneTimeResend resend = new OneTimeResend(websocket, req, options.getRetryResendAfter(), sub);
            secondResends.put(sub.getId(), resend);
            resend.start();
        }
    }

    private void handleUnsubcribeResponse(UnsubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.UNSUBSCRIBED);
    }

    private void handleResendResponseResending(ResendResponseResending res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        log.debug("Resending started for subscription "+sub.getId());
    }

    private void handleResendResponseNoResend(ResendResponseNoResend res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        endResendAndCheckQueue(sub);
        sub.resendDone();
    }

    private void handleResendResponseResent(ResendResponseResent res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        endResendAndCheckQueue(sub);
        sub.resendDone();
    }

    private void endResendAndCheckQueue(Subscription sub) {
        try {
            sub.endResend();
        } catch (GapDetectedException e) {
            ResendRangeRequest req = new ResendRangeRequest(e.getStreamId(), e.getStreamPartition(),
                sub.getId(), e.getFrom(), e.getTo(), e.getPublisherId(), e.getMsgChainId(), getSessionToken());
            sub.startResend();
            this.websocket.send(req.toJson());
            new PeriodicResend(websocket, options.getGapFillTimeout(), sub, e.getPublisherId(),
                    e.getMsgChainId(), getSessionToken()).start();
        }
    }
}
