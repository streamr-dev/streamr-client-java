package com.streamr.client;

import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.authentication.ChallengeAuthenticationMethod;
import com.streamr.client.protocol.control_layer.*;
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
import com.streamr.client.utils.Subscriptions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.Date;
import java.util.Map;

/**
 * Extends the AbstractStreamrClient with methods for using the websocket protocol
 */
public class StreamrWebsocketClient extends StreamrClient {

    private static final Logger log = LogManager.getLogger();

    // Underlying websocket implementation
    private final WebSocketClient websocket;

    private final Subscriptions subs = new Subscriptions();

    public enum State {
        Connecting, Connected, Disconnecting, Disconnected
    }

    private State state = State.Disconnected;
    private Exception errorWhileConnecting = null;

    private String publisherId = null;
    private final MessageCreationUtil msgCreationUtil;

    public StreamrWebsocketClient(StreamrClientOptions options) {
        super(options);

        if (options.getAuthenticationMethod() instanceof ApiKeyAuthenticationMethod) {
            try {
                publisherId = DigestUtils.sha256Hex(getUserInfo().getUsername());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (options.getAuthenticationMethod() instanceof ChallengeAuthenticationMethod) {
            publisherId = ((ChallengeAuthenticationMethod) options.getAuthenticationMethod()).getAddress();
        }
        msgCreationUtil = new MessageCreationUtil(publisherId);

        try {
            this.websocket = new WebSocketClient(new URI(options.getWebsocketApiUrl())) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Connection established");
                    state = State.Connected;
                    StreamrWebsocketClient.this.onOpen();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Connection closed! Code: " + code + ", Reason: " + reason);
                    state = State.Disconnected;
                    StreamrWebsocketClient.this.onClose();
                }

                @Override
                public void onError(Exception ex) {
                    log.error(ex);

                    if (state == State.Connecting) {
                        errorWhileConnecting = ex;
                    }

                    StreamrWebsocketClient.this.onError(ex);
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

    /*
     * Message handling
     */

    public void handleMessage(String rawMessageAsString) {
        try {
            log.info("<< " + rawMessageAsString);

            // Handle different message types
            ControlMessage message = ControlMessage.fromJson(rawMessageAsString);
            if (message != null) {
                try {
                    if (message.getType() == BroadcastMessage.TYPE) {
                        BroadcastMessage msg = (BroadcastMessage) message;
                        handleMessage(msg.getStreamMessage());
                    } else if (message.getType() == UnicastMessage.TYPE) {
                        UnicastMessage msg = (UnicastMessage) message;
                        handleMessage(msg.getStreamMessage());
                    } else if (message.getType() == SubscribeResponse.TYPE) {
                        handleSubcribeResponse((SubscribeResponse)message);
                    } else if (message.getType() == UnsubscribeResponse.TYPE) {
                        handleUnsubcribeResponse((UnsubscribeResponse)message);
                    }
                } catch (Exception e) {
                    log.error("Error handling message: " + message, e);
                }
            } else {
                log.error("Parsed message was null! Raw message: " + rawMessageAsString);
            }
        } catch (IOException e) {
            log.error("Error while handling message: " + rawMessageAsString, e);
        }
    }

    private void handleMessage(StreamMessage message) throws SubscriptionNotFoundException {
        // TODO: gap checking and gap fill
        log.debug(message.getStreamId() + ": " + message.getContent().toString());

        Subscription sub = subs.get(message.getStreamId(), message.getStreamPartition());

        // Only call the handler if we are in subscribed state (and not for example UNSUBSCRIBING)
        if (sub.getState().equals(Subscription.State.SUBSCRIBED)) {
            sub.getHandler().onMessage(sub, message);
        }
    }

    /*
     * Publish
     */

    public void publish(Stream stream, Map<String, Object> payload) {
        publish(stream, payload, new Date());
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp) {
        if (!getState().equals(StreamrWebsocketClient.State.Connected)) {
            connect();
        }
        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, timestamp, null);
        PublishRequest req = new PublishRequest(streamMessage, getSessionToken());
        getWebsocket().send(req.toJson());
    }

    /*
     * Subscribe
     */

    public Subscription subscribe(Stream stream, MessageHandler handler) {
        if (stream.getPartitions() > 1) {
            throw new PartitionNotSpecifiedException(stream.getId(), stream.getPartitions());
        }
        return subscribe(stream, 0, handler);
    }

    public Subscription subscribe(Stream stream, int partition, MessageHandler handler) {
        if (!getState().equals(State.Connected)) {
            connect();
        }

        SubscribeRequest subscribeRequest = new SubscribeRequest(stream.getId(), partition, session.getSessionToken());
        Subscription sub = new Subscription(stream.getId(), partition, handler);
        subs.add(sub);
        sub.setState(Subscription.State.SUBSCRIBING);
        this.websocket.send(subscribeRequest.toJson());
        return sub;
    }

    /*
     * Unsubscribe
     */

    public void unsubscribe(Subscription sub) {
        UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest(sub.getStreamId(), sub.getPartition());
        sub.setState(Subscription.State.UNSUBSCRIBING);
        this.websocket.send(unsubscribeRequest.toJson());
    }

    private void handleSubcribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.SUBSCRIBED);
    }

    private void handleUnsubcribeResponse(UnsubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.UNSUBSCRIBED);
    }

}
