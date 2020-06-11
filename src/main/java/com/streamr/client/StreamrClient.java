package com.streamr.client;

import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.exceptions.*;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.*;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.*;
import com.streamr.client.utils.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Extends the StreamrRESTClient with methods for using the websocket protocol.
 */
public class StreamrClient extends StreamrRESTClient {

    private static final Logger log = LogManager.getLogger();

    // Underlying websocket implementation
    private WebSocketClient websocket;

    protected final Subscriptions subs = new Subscriptions();

    private String publisherId = null;
    private final EncryptionUtil encryptionUtil;
    private final MessageCreationUtil msgCreationUtil;
    private final SubscribedStreamsUtil subscribedStreamsUtil;
    private final KeyStorage keyStorage;
    private final KeyExchangeUtil keyExchangeUtil;

    private Stream keyExchangeStream;
    private Subscription keyExchangeSub;

    private final HashMap<String, OneTimeResend> secondResends = new HashMap<>();

    private ErrorMessageHandler errorMessageHandler;
    private boolean keepConnected = false;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Object stateChangeLock = new Object();

    public StreamrClient(StreamrClientOptions options) {
        super(options);
        AddressValidityUtil addressValidityUtil = new AddressValidityUtil(streamId -> {
            try {
                return getSubscribers(streamId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, (streamId, subscriberAddress) -> {
            try {
                return isSubscriber(streamId, subscriberAddress);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, streamId -> {
            try {
                return getPublishers(streamId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, (streamId, publisherAddress) -> {
            try {
                return isPublisher(streamId, publisherAddress);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        subscribedStreamsUtil = new SubscribedStreamsUtil(id -> {
            try {
                return getStream(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, addressValidityUtil, options.getSigningOptions().getVerifySignatures());

        if (options.getAuthenticationMethod() instanceof ApiKeyAuthenticationMethod) {
            try {
                UserInfo info = getUserInfo();
                publisherId = DigestUtils.sha256Hex(info.getUsername() == null ? info.getId() : info.getUsername());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (options.getAuthenticationMethod() instanceof EthereumAuthenticationMethod) {
            publisherId = ((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAddress();

            // The key exchange stream is a system stream.
            // It doesn't explicitly exist, but as per spec, we can subscribe to it anyway.
            keyExchangeStream = new Stream("Inbox stream for " + publisherId, "");
            keyExchangeStream.setId("SYSTEM/keyexchange/"+publisherId);
            keyExchangeStream.setPartitions(1);
        }
        SigningUtil signingUtil = null;
        if (options.getPublishSignedMsgs()) {
            signingUtil = new SigningUtil(((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAccount());
        }
        HashMap<String, UnencryptedGroupKey> publisherKeys = options.getEncryptionOptions().getPublisherGroupKeys();
        keyStorage = options.getEncryptionOptions().getPublisherStoreKeyHistory() ? new KeyHistoryStorage(publisherKeys)
                : new LatestKeyStorage(publisherKeys);
        msgCreationUtil = new MessageCreationUtil(publisherId, signingUtil, keyStorage);
        encryptionUtil = new EncryptionUtil(options.getEncryptionOptions().getRsaPublicKey(),
                options.getEncryptionOptions().getRsaPrivateKey());
        keyExchangeUtil = new KeyExchangeUtil(keyStorage, msgCreationUtil, encryptionUtil, addressValidityUtil,
                this::publish, this::setGroupKeys);
    }

    public StreamrClient(AuthenticationMethod authenticationMethod) {
        this(new StreamrClientOptions(authenticationMethod));
    }

    private void initWebsocket() {
        try {
            this.websocket = new WebSocketClient(new URI(options.getWebsocketApiUrl())) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Connection established");
                    StreamrClient.this.onOpen();
                    try {
                        StreamrClient.this.subs.forEach(StreamrClient.this::resubscribe);
                    } catch (WebsocketNotConnectedException e) {
                        log.error(e);
                    }
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Connection closed! Code: " + code + ", Reason: " + reason);
                    if (!keepConnected) {
                        StreamrClient.this.onClose();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error(ex);
                    if (!(ex instanceof IOException)) {
                        StreamrClient.this.onError(ex);
                    }
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
    public void onClose() {
        subscribedStreamsUtil.clearAndClose();
    }
    public void onError(Exception ex) {}

    public WebSocketClient getWebsocket() {
        return websocket;
    }

    /**
     * Connects the websocket. Blocks until connected, or throws if the connection times out.
     */
    public void connect() throws ConnectionTimeoutException {
        if (getState() == ReadyState.OPEN) {
            return;
        }

        synchronized (stateChangeLock) {
            if (!keepConnected) {
                keepConnected = true;
                log.info("Connecting to " + options.getWebsocketApiUrl() + "...");
                executorService.scheduleAtFixedRate(() -> {
                            if (keepConnected) {
                                if (getState() != ReadyState.OPEN) {
                                    boolean isReconnect = this.websocket != null;
                                    log.info("Not connected. Attempting to " + (isReconnect ? "reconnect" : "connect"));
                                    if (isReconnect) {
                                        this.websocket.closeConnection(0, "");
                                    }
                                    initWebsocket();
                                    this.websocket.connect();
                                }
                            } else {
                                if (getState() != ReadyState.CLOSED) {
                                    log.info("Closing connection");
                                    websocket.closeConnection(0, "");
                                    websocket = null;
                                    executorService.shutdown();
                                }
                            }
                        },
                        0,
                        options.getReconnectRetryInterval(),
                        TimeUnit.MILLISECONDS
                );
            }
        }

        waitForState(ReadyState.OPEN);
        if (getState() != ReadyState.OPEN) {
            throw new ConnectionTimeoutException(options.getWebsocketApiUrl());
        }

        if (keyExchangeStream != null && keyExchangeSub == null) {
            keyExchangeSub = subscribe(keyExchangeStream, new MessageHandler() {
                @Override
                public void onMessage(Subscription sub, StreamMessage message) {
                    try {
                        if (message.getContentType().equals(StreamMessage.ContentType.GROUP_KEY_REQUEST)) {
                            keyExchangeUtil.handleGroupKeyRequest(message);
                        } else if (message.getContentType().equals(StreamMessage.ContentType.GROUP_KEY_RESPONSE_SIMPLE)) {
                            keyExchangeUtil.handleGroupKeyResponse(message);
                        } else if (message.getContentType().equals(StreamMessage.ContentType.GROUP_KEY_RESET_SIMPLE)) {
                            keyExchangeUtil.handleGroupKeyReset(message);
                        } else if (message.getContentType().equals(StreamMessage.ContentType.ERROR_MSG)) {
                            handleInboxStreamErrorMessage(message);
                        } else {
                            throw new MalformedMessageException("Cannot handle message with content type: " + message.getContentType());
                        }
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                        // we don't notify the error to the originator if the message is unauthenticated.
                        if (message.getSignature() != null) {
                            StreamMessage errorMessage = msgCreationUtil.createErrorMessage(message.getPublisherId(), e);
                            publish(errorMessage); //sending the error to the sender of 'message'
                        }
                    }
                }
            });
        }
        log.info("Connected to " + options.getWebsocketApiUrl());
    }

    private void handleInboxStreamErrorMessage(StreamMessage message) throws IOException {
        Map<String, Object> content = message.getContent();
        log.warn("Received error of type " + content.get("code") + " from " + message.getPublisherId() + ": " + content.get("message"));
    }

    /**
     * Disconnects the websocket. Blocks until disconnected, or throws if the operation times out.
     */
    public void disconnect() throws ConnectionTimeoutException {
        if (getState() == ReadyState.CLOSED) {
            return;
        }

        synchronized (stateChangeLock) {
            keepConnected = false;
        }
        waitForState(ReadyState.CLOSED);
        ReadyState state = getState();
        if (state != ReadyState.CLOSED) {
            throw new RuntimeException(String.format(
                    "Failed to disconnect: never went from %s to CLOSED readyState",
                    state
            ));
        }
    }

    public void setErrorMessageHandler(ErrorMessageHandler errorMessageHandler) {
        this.errorMessageHandler = errorMessageHandler;
    }

    private void waitForState(ReadyState target) {
        long maxWaitTime = options.getReconnectRetryInterval() + options.getConnectionTimeoutMillis() + 500;
        long timeWaited = 0;
        while (getState() != target && timeWaited < maxWaitTime) {
            try {
                Thread.sleep(100);
                timeWaited += 100;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void send(ControlMessage message) {
        this.websocket.send(message.toJson());
    }

    public ReadyState getState() {
        return this.websocket == null ? ReadyState.CLOSED : this.websocket.getReadyState();
    }

    public String getPublisherId() {
        return publisherId;
    }

    /*
     * Message handling
     */

    protected void handleMessage(String rawMessageAsString) {
        try {
            log.info(getPublisherId() + " << " + rawMessageAsString);

            // Handle different message types
            ControlMessage message = ControlMessage.fromJson(rawMessageAsString);
            if (message != null) {
                try {
                    if (message.getType() == BroadcastMessage.TYPE) {
                        BroadcastMessage msg = (BroadcastMessage) message;
                        handleMessage(msg.getStreamMessage(), Subscription::handleRealTimeMessage);
                    } else if (message.getType() == UnicastMessage.TYPE) {
                        UnicastMessage msg = (UnicastMessage) message;
                        handleMessage(msg.getStreamMessage(), Subscription::handleResentMessage);
                    } else if (message.getType() == SubscribeResponse.TYPE) {
                        handleSubscribeResponse((SubscribeResponse)message);
                    } else if (message.getType() == UnsubscribeResponse.TYPE) {
                        handleUnsubcribeResponse((UnsubscribeResponse)message);
                    } else if (message.getType() == ResendResponseResending.TYPE) {
                        handleResendResponseResending((ResendResponseResending)message);
                    } else if (message.getType() == ResendResponseNoResend.TYPE) {
                        handleResendResponseNoResend((ResendResponseNoResend)message);
                    } else if (message.getType() == ResendResponseResent.TYPE) {
                        handleResendResponseResent((ResendResponseResent)message);
                    } else if (message.getType() == ErrorResponse.TYPE) {
                        ErrorResponse error = (ErrorResponse) message;
                        if (this.errorMessageHandler != null) {
                            this.errorMessageHandler.onErrorMessage(error);
                        } else {
                            log.error("Protocol error message: '{}'", error.getErrorMessage());
                        }
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

    private void handleMessage(StreamMessage message,
                               BiConsumer<Subscription, StreamMessage> subMsgHandler) throws SubscriptionNotFoundException {
        log.debug(message.getStreamId() + ": " + message.getSerializedContent());

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
            subMsgHandler.accept(sub, message);
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

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        publish(stream, payload, timestamp, partitionKey, null);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey, UnencryptedGroupKey newGroupKey) {
        // Convenience feature: allow user to call publish() without having had called connect() beforehand.
        connect();
        if (newGroupKey != null) {
            options.getEncryptionOptions().getPublisherGroupKeys().put(stream.getId(), newGroupKey);
        }
        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, timestamp, partitionKey, newGroupKey);
        if (options.getEncryptionOptions().autoRevoke() && keyExchangeUtil.keyRevocationNeeded(stream.getId())) {
            keyExchangeUtil.rekey(stream.getId(), true);
        }
        try {
            publish(streamMessage);
        } catch (WebsocketNotConnectedException e) {
            // TODO: re-try sending once. Need to implement proper message enqueuing while not connected later.
            connect();
            publish(streamMessage);
        }
    }

    private void publish(StreamMessage streamMessage) {
        PublishRequest req = new PublishRequest(newRequestId(), streamMessage, getSessionToken());
        getWebsocket().send(req.toJson());
    }

    public void rekey(Stream stream) {
        keyExchangeUtil.rekey(stream.getId(), false);
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
                                  Map<String, UnencryptedGroupKey> groupKeys) {
        return subscribe(stream, partition, handler, resendOption, groupKeys, false);
    }

    public Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption,
                                  Map<String, UnencryptedGroupKey> groupKeys, boolean isExplicitResend) {
        if (!getState().equals(ReadyState.OPEN)) {
            connect();
        }

        Map<String, UnencryptedGroupKey> keysPerPublisher = getKeysPerPublisher(stream.getId());
        if (groupKeys != null) {
            keysPerPublisher.putAll(groupKeys);
        }

        SubscribeRequest subscribeRequest = new SubscribeRequest(newRequestId(), stream.getId(), partition, getSessionToken());

        Subscription sub;
        BasicSubscription.GroupKeyRequestFunction requestFunction = (publisherId, start, end) -> sendGroupKeyRequest(stream.getId(), publisherId, start, end);
        if (resendOption == null) {
            sub = new RealTimeSubscription(stream.getId(), partition, handler, keysPerPublisher,
                    requestFunction, options.getPropagationTimeout(), options.getResendTimeout(),
                    options.getSkipGapsOnFullQueue());
        } else if (isExplicitResend) {
            sub = new HistoricalSubscription(stream.getId(), partition, handler, resendOption, keysPerPublisher,
                    requestFunction, options.getPropagationTimeout(), options.getResendTimeout(),
                    options.getSkipGapsOnFullQueue());
        } else {
            sub = new CombinedSubscription(stream.getId(), partition, handler, resendOption, keysPerPublisher, requestFunction,
                    options.getPropagationTimeout(), options.getResendTimeout(), options.getSkipGapsOnFullQueue());
        }
        sub.setGapHandler((MessageRef from, MessageRef to, String publisherId, String msgChainId) -> {
            ResendRangeRequest req = new ResendRangeRequest(
                    newRequestId(),
                    stream.getId(),
                    partition,
                    from,
                    to,
                    publisherId,
                    msgChainId,
                    getSessionToken()
            );
            sub.setResending(true);
            send(req);
        });
        subs.add(sub);
        sub.setState(Subscription.State.SUBSCRIBING);
        send(subscribeRequest);
        return sub;
    }

    private void resubscribe(Subscription sub) {
        SubscribeRequest subscribeRequest = new SubscribeRequest(newRequestId(), sub.getStreamId(), sub.getPartition(), getSessionToken());
        sub.setState(Subscription.State.SUBSCRIBING);
        send(subscribeRequest);
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

        subscribe(stream, partition, a, resendOption, null, true);
    }

    /*
     * Unsubscribe
     */

    public void unsubscribe(Subscription sub) {
        UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest(newRequestId(), sub.getStreamId(), sub.getPartition());
        sub.setState(Subscription.State.UNSUBSCRIBING);
        sub.setResending(false);
        send(unsubscribeRequest);
    }

    private void handleSubscribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.SUBSCRIBED);
        if (sub.hasResendOptions()) {
            ResendOption resendOption = sub.getResendOption();
            ControlMessage req = resendOption.toRequest(newRequestId(), res.getStreamId(), res.getStreamPartition(), this.getSessionToken());
            send(req);
            OneTimeResend resend = new OneTimeResend(websocket, req, options.getResendTimeout(), sub);
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
        sub.startResend();
        log.debug("Resending started for subscription "+sub.getId());
    }

    private void handleResendResponseNoResend(ResendResponseNoResend res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.endResend();
    }

    private void handleResendResponseResent(ResendResponseResent res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.endResend();
    }

    private HashMap<String, UnencryptedGroupKey> getKeysPerPublisher(String streamId) {
        if (!options.getEncryptionOptions().getSubscriberGroupKeys().containsKey(streamId)) {
            options.getEncryptionOptions().getSubscriberGroupKeys().put(streamId, new HashMap<>());
        }
        return options.getEncryptionOptions().getSubscriberGroupKeys().get(streamId);
    }

    private void setGroupKeys(String streamId, String publisherId, ArrayList<UnencryptedGroupKey> keys) throws UnableToSetKeysException {
        UnencryptedGroupKey current = getKeysPerPublisher(streamId).get(publisherId);
        UnencryptedGroupKey last = keys.get(keys.size() - 1);
        if (current == null || current.getStartTime() < last.getStartTime()) {
            getKeysPerPublisher(streamId).put(publisherId, last);
        }
        for (Subscription sub: subs.getAllForStreamId(streamId)) {
            sub.setGroupKeys(publisherId, keys);
        }
    }

    private void sendGroupKeyRequest(String streamId, String publisherId, Date start, Date end) {
        if (!getState().equals(ReadyState.OPEN)) {
            connect();
        }
        StreamMessage request = msgCreationUtil.createGroupKeyRequest(publisherId, streamId, encryptionUtil.getPublicKeyAsPemString(), start, end);
        publish(request);
    }

    private String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
