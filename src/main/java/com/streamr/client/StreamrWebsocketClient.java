package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.streamr.client.exceptions.AlreadySubscribedException;
import com.streamr.client.exceptions.ConnectionTimeoutException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.protocol.*;
import com.streamr.client.utils.Subscriptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

public class StreamrWebsocketClient {

    private static final Logger log = LogManager.getLogger();

    // Thread safe
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<SubscribeRequest> subscribeRequestAdapter = moshi.adapter(SubscribeRequest.class);
    private final JsonAdapter<UnsubscribeRequest> unsubscribeRequestAdapter = moshi.adapter(UnsubscribeRequest.class);
    private final JsonAdapter<MessageFromServer> messageFromServerAdapter = new MessageFromServerAdapter();

    private final StreamrClientOptions options;
    private final WebSocketClient websocket;

    private final Subscriptions subs = new Subscriptions();

    public enum State {
        Connecting, Connected, Disconnecting, Disconnected
    }

    private State state = State.Disconnected;
    private Exception errorWhileConnecting = null;

    public StreamrWebsocketClient() {
        this(new StreamrClientOptions());
    }

    public StreamrWebsocketClient(StreamrClientOptions options) {
        this.options = options;

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

    public void onOpen() {}
    public void onClose() {}
    public void onError(Exception ex) {}

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

    public void handleMessage(String rawMessageAsString) {
        try {
            log.debug("<< " + rawMessageAsString);

            // Handle different message types
            MessageFromServer message = messageFromServerAdapter.fromJson(rawMessageAsString);
            if (message != null) {
                Object payload = message.getPayload();

                try {
                    if (payload instanceof StreamMessage) {
                        handleMessage((StreamMessage) payload);
                    } else if (payload instanceof SubscribeResponse) {
                        handleSubcribeResponse((SubscribeResponse) payload);
                    } else if (payload instanceof UnsubscribeResponse) {
                        handleUnsubcribeResponse((UnsubscribeResponse) payload);
                    }
                } catch (Exception e) {
                    log.error("Error handling message payload: " + payload, e);
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
        log.debug(message.getStreamId() + ": " + message.getPayload().toString());

        Subscription sub = subs.get(message.getStreamId(), message.getPartition());

        // Only call the handler if we are in subscribed state (and not for example UNSUBSCRIBING)
        if (sub.getState().equals(Subscription.State.SUBSCRIBED)) {
            sub.getHandler().onMessage(sub, message);
        }
    }

    public Subscription subscribe(String streamId, MessageHandler handler) {
        return subscribe(streamId, 0, options.getApiKey(), handler);
    }

    public Subscription subscribe(String streamId, String apiKey, MessageHandler handler) {
        return subscribe(streamId, 0, apiKey, handler);
    }

    public Subscription subscribe(String streamId, int partition, MessageHandler handler) {
        return subscribe(streamId, partition, options.getApiKey(), handler);
    }

    public Subscription subscribe(String streamId, int partition, String apiKey, MessageHandler handler) throws AlreadySubscribedException {
        String subscribeRequest = subscribeRequestAdapter.toJson(new SubscribeRequest(streamId, partition, apiKey));
        Subscription sub = new Subscription(streamId, partition, handler);
        subs.add(sub);
        sub.setState(Subscription.State.SUBSCRIBING);
        this.websocket.send(subscribeRequest);
        return sub;
    }

    public void unsubscribe(Subscription sub) {
        String unsubscribeRequest = unsubscribeRequestAdapter.toJson(new UnsubscribeRequest(sub.getStreamId(), sub.getPartition()));
        sub.setState(Subscription.State.UNSUBSCRIBING);
        this.websocket.send(unsubscribeRequest);
    }

    private void handleSubcribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStream(), res.getPartition());
        sub.setState(Subscription.State.SUBSCRIBED);
    }

    private void handleUnsubcribeResponse(UnsubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStream(), res.getPartition());
        sub.setState(Subscription.State.UNSUBSCRIBED);
    }

    // TODO: remove
    public static void main(String[] args) throws InterruptedException {
        StreamrClientOptions options = new StreamrClientOptions();
        //options.setApiKey("tester1-api-key");
        //options.setWebsocketApiUrl("ws://localhost:8890/api/v1/ws");

        StreamrWebsocketClient client = new StreamrWebsocketClient(options);
        try {
            client.connect();
        } catch (ConnectionTimeoutException e) {
            log.error(e);
        }

        System.out.println("state: " + client.getState());

        Subscription sub = client.subscribe("7wa7APtlTq6EC5iTCBy6dw", new MessageHandler() {
            @Override
            public void onMessage(Subscription sub, StreamMessage message) {
                log.info("Message received! " + message.getPayload());
            }
        });

        Thread.sleep(10*1000);

        client.unsubscribe(sub);
    }

}
