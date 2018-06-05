package com.streamr.client;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.streamr.client.exceptions.AlreadySubscribedException;
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

public class StreamrWebsocketClient extends WebSocketClient {

    private static final Logger log = LogManager.getLogger();

    // Thread safe
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<SubscribeRequest> subscribeRequestAdapter = moshi.adapter(SubscribeRequest.class);
    private final JsonAdapter<UnsubscribeRequest> unsubscribeRequestAdapter = moshi.adapter(UnsubscribeRequest.class);
    private final JsonAdapter<MessageFromServer> messageFromServerAdapter = moshi.adapter(MessageFromServer.class);

    private final StreamrClientOptions options;

    private final Subscriptions subs = new Subscriptions();

    public StreamrWebsocketClient(StreamrClientOptions options) throws URISyntaxException {
        super(new URI(options.getWebsocketApiUrl()));
        this.options = options;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        log.info("Connection established");
    }

    @Override
    public void onMessage(String rawMessageAsString) {
        try {
            log.info("<< " + rawMessageAsString);

            // Handle different message types
            MessageFromServer message = messageFromServerAdapter.fromJson(rawMessageAsString);
            if (message != null) {
                Object payload = message.getPayload();

                try {
                    // TODO: other types
                    if (payload instanceof SubscribeResponse) {
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
            log.error(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Connection closed! Code: " + code +", Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        log.error(ex);
    }

    public Subscription subscribe(String streamId) {
        return subscribe(streamId, 0, options.getApiKey());
    }

    public Subscription subscribe(String streamId, String apiKey) {
        return subscribe(streamId, 0, apiKey);
    }

    public Subscription subscribe(String streamId, int partition) {
        return subscribe(streamId, partition, options.getApiKey());
    }

    public Subscription subscribe(String streamId, int partition, String apiKey) throws AlreadySubscribedException {
        String subscribeRequest = subscribeRequestAdapter.toJson(new SubscribeRequest(streamId, partition, apiKey));
        Subscription sub = new Subscription(streamId, partition);
        subs.add(sub);
        sub.setState(Subscription.State.SUBSCRIBING);
        this.send(subscribeRequest);
        return sub;
    }

    public void unsubscribe(Subscription sub) {
        String unsubscribeRequest = unsubscribeRequestAdapter.toJson(new UnsubscribeRequest(sub.getStreamId(), sub.getPartition()));
        sub.setState(Subscription.State.UNSUBSCRIBING);
        this.send(unsubscribeRequest);
    }

    private void handleSubcribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(new Subscription(res.getStream(), res.getPartition()));
        sub.setState(Subscription.State.SUBSCRIBED);
    }

    private void handleUnsubcribeResponse(UnsubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(new Subscription(res.getStream(), res.getPartition()));
        sub.setState(Subscription.State.UNSUBSCRIBED);
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        StreamrClientOptions options = new StreamrClientOptions();
        options.setApiKey("tester1-api-key");
        options.setWebsocketApiUrl("ws://localhost:8890/api/v1/ws");

        StreamrWebsocketClient client = new StreamrWebsocketClient(options);
        client.connect();
        Thread.sleep(5*1000);
        System.out.println("Open: "+client.isOpen());
        System.out.println("isConnecting: "+client.isConnecting());

        client.subscribe("qpR_ru-oTIyEwZ86j7mYEQ");

    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        log.info(">> " + text);
        super.send(text);
    }

}

