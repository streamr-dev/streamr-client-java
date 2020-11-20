package com.streamr.client;

import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.authentication.AuthenticationMethod;
import com.streamr.client.authentication.EthereumAuthenticationMethod;
import com.streamr.client.dataunion.DataUnionClient;
import com.streamr.client.exceptions.ConnectionTimeoutException;
import com.streamr.client.exceptions.MalformedMessageException;
import com.streamr.client.exceptions.PartitionNotSpecifiedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.control_layer.*;
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.*;
import com.streamr.client.utils.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Extends the StreamrRESTClient with methods for using the websocket protocol.
 */
public class StreamrClient extends StreamrRESTClient {

    private static final Logger log = LoggerFactory.getLogger(StreamrClient.class);

    // Underlying websocket implementation
    private WebSocketClient websocket;

    protected final Subscriptions subs = new Subscriptions();

    private Address publisherId = null;
    private final EncryptionUtil encryptionUtil;
    private final MessageCreationUtil msgCreationUtil;
    private final StreamMessageValidator streamMessageValidator;
    private final GroupKeyStore keyStore;
    private final KeyExchangeUtil keyExchangeUtil;

    private Stream keyExchangeStream;
    private Subscription keyExchangeSub;

    private final HashMap<String, OneTimeResend> secondResends = new HashMap<>();

    private ErrorMessageHandler errorMessageHandler;
    private boolean keepConnected = false;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Object stateChangeLock = new Object();
    private int requestCounter = 0;

    public StreamrClient(StreamrClientOptions options) {
        super(options);
        AddressValidityUtil addressValidityUtil = new AddressValidityUtil(streamId -> {
            try {
                return getSubscribers(streamId).stream().map(Address::new).collect(Collectors.toList());
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
                return getPublishers(streamId).stream().map(Address::new).collect(Collectors.toList());
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
        streamMessageValidator = new StreamMessageValidator(id -> {
            try {
                return getStream(id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, addressValidityUtil, options.getSigningOptions().getVerifySignatures());

        if (options.getAuthenticationMethod() instanceof ApiKeyAuthenticationMethod) {
            try {
                UserInfo info = getUserInfo();
                publisherId = new Address(DigestUtils.sha256Hex(info.getUsername() == null ? info.getId() : info.getUsername()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (options.getAuthenticationMethod() instanceof EthereumAuthenticationMethod) {
            publisherId = new Address(((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAddress());

            // The key exchange stream is a system stream.
            // It doesn't explicitly exist, but as per spec, we can subscribe to it anyway.
            keyExchangeStream = new Stream("Key exchange stream for " + publisherId, "");
            keyExchangeStream.setId(KeyExchangeUtil.getKeyExchangeStreamId(publisherId));
            keyExchangeStream.setPartitions(1);
        }
        SigningUtil signingUtil = null;
        if (options.getPublishSignedMsgs()) {
            signingUtil = new SigningUtil(((EthereumAuthenticationMethod) options.getAuthenticationMethod()).getAccount());
        }

        // Create key storage
        keyStore = options.getEncryptionOptions().getKeyStore();

        msgCreationUtil = new MessageCreationUtil(publisherId, signingUtil);
        encryptionUtil = new EncryptionUtil(options.getEncryptionOptions().getRsaPublicKey(),
                options.getEncryptionOptions().getRsaPrivateKey());
        keyExchangeUtil = new KeyExchangeUtil(keyStore, msgCreationUtil, encryptionUtil, addressValidityUtil,
                this::publish,
                // On new keys, let the Subscriptions know
                (streamId, publisherId, keys) -> {
                    for (Subscription sub: subs.getAllForStreamId(streamId)) {
                        sub.onNewKeysAdded(publisherId, keys);
                    }
                });
    }

    public StreamrClient(AuthenticationMethod authenticationMethod) {
        this(new StreamrClientOptions(authenticationMethod));
    }

    public StreamrClient() {
        this(new StreamrClientOptions());
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
                        log.error("Failed to resubscribe", e);
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
                    log.error("WebSocketClient#onError called", ex);
                    if (!(ex instanceof IOException)) {
                        StreamrClient.this.onError(ex);
                    }
                }

                @Override
                public void send(String text) throws NotYetConnectedException {
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
        streamMessageValidator.clearAndClose();
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
                        if (message.getMessageType().equals(StreamMessage.MessageType.GROUP_KEY_REQUEST)) {
                            try {
                                keyExchangeUtil.handleGroupKeyRequest(message);
                            } catch (Exception e) {
                                GroupKeyRequest groupKeyRequest = (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(message);
                                StreamMessage errorMessage = msgCreationUtil.createGroupKeyErrorResponse(message.getPublisherId(), groupKeyRequest, e);
                                publish(errorMessage); //sending the error to the sender of 'message'
                            }
                        } else if (message.getMessageType().equals(StreamMessage.MessageType.GROUP_KEY_RESPONSE)) {
                            keyExchangeUtil.handleGroupKeyResponse(message);
                        } else if (message.getMessageType().equals(StreamMessage.MessageType.GROUP_KEY_ANNOUNCE)) {
                            keyExchangeUtil.handleGroupKeyAnnounce(message);
                        } else if (message.getMessageType().equals(StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE)) {
                            Map<String, Object> content = message.getParsedContent();
                            log.warn("Received error of type " + content.get("code") + " from " + message.getPublisherId() + ": " + content.get("message"));
                        } else {
                            throw new MalformedMessageException("Unexpected message type on key exchange stream: " + message.getMessageType());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            });
        }
        log.info("Connected to " + options.getWebsocketApiUrl());
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
        log.trace("[{}] >> {}", publisherId != null ? publisherId.toString().substring(0, 6) : null, message);
        if (this.websocket != null) {
            this.websocket.send(message.toJson());
        } else {
            log.warn("send: websocket is null, not sending message {}", message);
        }
    }

    public ReadyState getState() {
        return this.websocket == null ? ReadyState.CLOSED : this.websocket.getReadyState();
    }

    public Address getPublisherId() {
        return publisherId;
    }

    public GroupKeyStore getKeyStore() {
        return keyStore;
    }

    public DataUnionClient dataUnionClient(String mainnetAdminPrvKey, String sidechainAdminPrvKey) {
        return new DataUnionClient(options.getMainnetRpcUrl(),
                options.getDataUnionMainnetFactoryAddress(),
                mainnetAdminPrvKey,
                options.getSidechainRpcUrl(),
                options.getDataUnionSidechainFactoryAddress(),
                sidechainAdminPrvKey);
    }

    /*
     * Message handling
     */

    protected void handleMessage(String rawMessageAsString) {
        try {
            // Handle different message types
            ControlMessage message = ControlMessage.fromJson(rawMessageAsString);

            log.trace("[{}] << {}", publisherId != null ? publisherId.toString().substring(0, 6) : null, message);

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
        streamMessageValidator.validate(message);
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
        publish(stream, payload, new Date(), null, null);
    }

    public void publish(Stream stream, Map<String, Object> payload, GroupKey groupKey) {
        publish(stream, payload, new Date(), null, groupKey);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp) {
        publish(stream, payload, timestamp, null, null);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, GroupKey groupKey) {
        publish(stream, payload, timestamp, null, groupKey);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        publish(stream, payload, timestamp, partitionKey, null);
    }

    public void publish(Stream stream, Map<String, Object> payload, Date timestamp, @Nullable String partitionKey, @Nullable GroupKey newGroupKey) {
        // Convenience feature: allow user to call publish() without having had called connect() beforehand.
        connect();

        GroupKey currentKey = keyStore.getCurrentKey(stream.getId());

        // Use the new key if there wasn't one before
        if (currentKey == null && newGroupKey != null) {
            currentKey = newGroupKey;
            keyStore.add(stream.getId(), newGroupKey);
        }

        // Ignore newGroupKey if it's the same as the current one
        if (currentKey != null && newGroupKey != null && currentKey.equals(newGroupKey)) {
            newGroupKey = null;
        }

        // Add the new key to the keyStore unless it's already there
        if (newGroupKey != null && keyStore.get(stream.getId(), newGroupKey.getGroupKeyId()) == null) {
            keyStore.add(stream.getId(), newGroupKey);
        }

        // Check if an automatic rekey is needed
        if (options.getEncryptionOptions().autoRevoke() && keyExchangeUtil.keyRevocationNeeded(stream.getId())) {
            keyExchangeUtil.rekey(stream.getId(), true);
        }

        StreamMessage streamMessage = msgCreationUtil.createStreamMessage(stream, payload, timestamp, partitionKey, currentKey, newGroupKey);
        try {
            publish(streamMessage);
        } catch (WebsocketNotConnectedException e) {
            // TODO: re-try sending once. Need to implement proper message enqueuing while not connected later.
            connect();
            publish(streamMessage);
        }
    }

    private void publish(StreamMessage streamMessage) {
        send(new PublishRequest(newRequestId("pub"), streamMessage, getSessionToken()));
    }

    public GroupKey rekey(Stream stream) {
        return keyExchangeUtil.rekey(stream.getId(), false);
    }

    /*
     * Subscribe
     */
    public Subscription subscribe(Stream stream, MessageHandler handler) {
        Integer nbPartitions = stream.getPartitions();
        if (nbPartitions != null && nbPartitions > 1) {
            throw new PartitionNotSpecifiedException(stream.getId(), stream.getPartitions());
        }
        return subscribe(stream, 0, handler, null, false);
    }

    public Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
        return subscribe(stream, partition, handler, resendOption, false);
    }

    protected Subscription subscribe(Stream stream, int partition, MessageHandler handler, ResendOption resendOption, boolean isExplicitResend) {
        if (!getState().equals(ReadyState.OPEN)) {
            connect();
        }

        SubscribeRequest subscribeRequest = new SubscribeRequest(newRequestId("sub"), stream.getId(), partition, getSessionToken());

        Subscription sub;
        BasicSubscription.GroupKeyRequestFunction requestFunction = (publisherId, groupKeyIds) -> sendGroupKeyRequest(stream.getId(), publisherId, groupKeyIds);
        if (resendOption == null) {
            sub = new RealTimeSubscription(stream.getId(), partition, handler, keyStore, keyExchangeUtil,
                    requestFunction, options.getPropagationTimeout(), options.getResendTimeout(),
                    options.getSkipGapsOnFullQueue());
        } else if (isExplicitResend) {
            sub = new HistoricalSubscription(stream.getId(), partition, handler, keyStore, keyExchangeUtil, resendOption,
                    requestFunction, options.getPropagationTimeout(), options.getResendTimeout(),
                    options.getSkipGapsOnFullQueue());
        } else {
            sub = new CombinedSubscription(stream.getId(), partition, handler, keyStore, keyExchangeUtil, resendOption, requestFunction,
                    options.getPropagationTimeout(), options.getResendTimeout(), options.getSkipGapsOnFullQueue());
        }
        sub.setGapHandler((MessageRef from, MessageRef to, Address publisherId, String msgChainId) -> {
            ResendRangeRequest req = new ResendRangeRequest(
                    newRequestId("resend"),
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
        SubscribeRequest subscribeRequest = new SubscribeRequest(newRequestId("resub"), sub.getStreamId(), sub.getPartition(), getSessionToken());
        sub.setState(Subscription.State.SUBSCRIBING);
        send(subscribeRequest);
    }

    /*
     * Resend
     */

    public void resend(Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
        MessageHandler a = new MessageHandler() {
            @Override
            public void onMessage(Subscription sub, StreamMessage message) {
                handler.onMessage(sub, message);
            }
            public void done(Subscription sub) {
                StreamrClient.this.unsubscribe(sub);
                handler.done(sub);
            }
        };

        subscribe(stream, partition, a, resendOption, true);
    }

    /*
     * Unsubscribe
     */

    public void unsubscribe(Subscription sub) {
        UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest(newRequestId("unsub"), sub.getStreamId(), sub.getPartition());
        sub.setState(Subscription.State.UNSUBSCRIBING);
        sub.setResending(false);
        send(unsubscribeRequest);
    }

    private void handleSubscribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
        Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
        sub.setState(Subscription.State.SUBSCRIBED);
        if (sub.hasResendOptions()) {
            ResendOption resendOption = sub.getResendOption();
            ControlMessage req = resendOption.toRequest(newRequestId("resend"), res.getStreamId(), res.getStreamPartition(), this.getSessionToken());
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

    private void sendGroupKeyRequest(String streamId, Address publisherId, List<String> groupKeyIds) {
        if (!getState().equals(ReadyState.OPEN)) {
            connect();
        }
        StreamMessage request = msgCreationUtil.createGroupKeyRequest(publisherId, streamId, encryptionUtil.getPublicKeyAsPemString(), groupKeyIds);
        publish(request);
    }

    private String newRequestId(String prefix) {
        return String.format("%s.%s.%d", prefix, IdGenerator.get(), requestCounter++);
    }
}
