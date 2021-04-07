package com.streamr.client;

import com.streamr.client.crypto.Keys;
import com.streamr.client.dataunion.DataUnionClient;
import com.streamr.client.exceptions.ConnectionTimeoutException;
import com.streamr.client.exceptions.PartitionNotSpecifiedException;
import com.streamr.client.exceptions.SubscriptionNotFoundException;
import com.streamr.client.java.util.Objects;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.control_layer.BroadcastMessage;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ErrorResponse;
import com.streamr.client.protocol.control_layer.PublishRequest;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import com.streamr.client.protocol.control_layer.ResendResponseNoResend;
import com.streamr.client.protocol.control_layer.ResendResponseResending;
import com.streamr.client.protocol.control_layer.ResendResponseResent;
import com.streamr.client.protocol.control_layer.SubscribeRequest;
import com.streamr.client.protocol.control_layer.SubscribeResponse;
import com.streamr.client.protocol.control_layer.UnicastMessage;
import com.streamr.client.protocol.control_layer.UnsubscribeRequest;
import com.streamr.client.protocol.control_layer.UnsubscribeResponse;
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.MalformedMessageException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageValidator;
import com.streamr.client.rest.AmbiguousResultsException;
import com.streamr.client.rest.Permission;
import com.streamr.client.rest.StorageNode;
import com.streamr.client.rest.Stream;
import com.streamr.client.rest.StreamPart;
import com.streamr.client.rest.StreamrRestClient;
import com.streamr.client.rest.UserInfo;
import com.streamr.client.subs.BasicSubscription;
import com.streamr.client.subs.CombinedSubscription;
import com.streamr.client.subs.HistoricalSubscription;
import com.streamr.client.subs.RealTimeSubscription;
import com.streamr.client.subs.Subscription;
import com.streamr.client.utils.Address;
import com.streamr.client.utils.AddressValidityUtil;
import com.streamr.client.utils.EncryptionUtil;
import com.streamr.client.utils.GroupKey;
import com.streamr.client.utils.GroupKeyStore;
import com.streamr.client.utils.IdGenerator;
import com.streamr.client.utils.KeyExchangeUtil;
import com.streamr.client.utils.MessageCreationUtil;
import com.streamr.client.utils.OneTimeResend;
import com.streamr.client.utils.Subscriptions;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extends the StreamrRestClient with methods for using the websocket protocol. */
public class StreamrClient implements Streamr {

  private static final Logger log = LoggerFactory.getLogger(StreamrClient.class);
  private final StreamrRestClient restClient;

  // Underlying websocket implementation
  private WebSocketClient websocket = null;
  private final ReadWriteLock websocketRwLock = new ReentrantReadWriteLock();
  private final Lock websocketRLock = this.websocketRwLock.readLock();
  private final Lock websocketWLock = this.websocketRwLock.writeLock();

  protected final Subscriptions subs = new Subscriptions();

  private Address publisherId = null;
  private final EncryptionUtil encryptionUtil;
  private final MessageCreationUtil msgCreationUtil;
  private final StreamMessageValidator streamMessageValidator;
  private final GroupKeyStore keyStore;
  private final KeyExchangeUtil keyExchangeUtil;

  private Stream keyExchangeStream;
  private Subscription keyExchangeSub;

  private final Map<String, OneTimeResend> secondResends = new HashMap<>();

  private ErrorMessageHandler errorMessageHandler;
  private boolean keepConnected = false;
  private final ReadWriteLock keepConnectedRwLock = new ReentrantReadWriteLock();
  private final Lock keepConnectedWLock = this.keepConnectedRwLock.writeLock();
  private final Lock keepConnectedRLock = this.keepConnectedRwLock.readLock();
  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor();
  private int requestCounter = 0;
  private final StreamrClientOptions options;

  public StreamrClientOptions getOptions() {
    return options;
  }

  public StreamrClient(StreamrClientOptions options, final StreamrRestClient restClient) {
    this.options = options;
    Objects.requireNonNull(restClient);
    this.restClient = restClient;
    AddressValidityUtil addressValidityUtil =
        new AddressValidityUtil(
            streamId -> {
              try {
                return getSubscribers(streamId).stream()
                    .map(Address::new)
                    .collect(Collectors.toList());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            (streamId, subscriberAddress) -> {
              try {
                return isSubscriber(streamId, subscriberAddress);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            streamId -> {
              try {
                return getPublishers(streamId).stream()
                    .map(Address::new)
                    .collect(Collectors.toList());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            (streamId, publisherAddress) -> {
              try {
                return isPublisher(streamId, publisherAddress);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    streamMessageValidator =
        new StreamMessageValidator(
            id -> {
              try {
                return getStream(id);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            addressValidityUtil,
            options.getSigningOptions().getVerifySignatures());

    BigInteger privateKey = restClient.getPrivateKey();
    if (privateKey != null) {
      publisherId = new Address(Keys.privateKeyToAddressWithPrefix(privateKey));

      // The key exchange stream is a system stream.
      // It doesn't explicitly exist, but as per spec, we can subscribe to it anyway.
      keyExchangeStream =
          new Stream.Builder()
              .withName("Key exchange stream for " + publisherId)
              .withDescription("")
              .withId(KeyExchangeUtil.getKeyExchangeStreamId(publisherId))
              .withPartitions(1)
              .createStream();
    }

    // Create key storage
    keyStore = options.getEncryptionOptions().getKeyStore();

    msgCreationUtil = new MessageCreationUtil(privateKey, publisherId);
    encryptionUtil =
        new EncryptionUtil(
            options.getEncryptionOptions().getRsaPublicKey(),
            options.getEncryptionOptions().getRsaPrivateKey());
    keyExchangeUtil =
        new KeyExchangeUtil(
            keyStore,
            msgCreationUtil,
            encryptionUtil,
            addressValidityUtil,
            this::publish,
            // On new keys, let the Subscriptions know
            (streamId, publisherId, keys) -> {
              for (Subscription sub : subs.getAllForStreamId(streamId)) {
                sub.onNewKeysAdded(publisherId, keys);
              }
            },
            Clock.systemDefaultZone());
  }

  private void initWebsocket() {
    final URI uri;
    try {
      final String websocketApiUrl = options.getWebsocketApiUrl();
      uri = new URI(websocketApiUrl);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    this.setWebsocket(new StreamrWebSocketClient(this, uri));
  }

  /*
   * Connecting and disconnecting
   */

  public void onOpen() {}

  public void onClose() {
    if (!isKeepConnected()) {
      return;
    }
    streamMessageValidator.clearAndClose();
  }

  public void onError(Exception ex) {}

  public WebSocketClient getWebsocket() {
    websocketRLock.lock();
    try {
      return this.websocket;
    } finally {
      websocketRLock.unlock();
    }
  }

  /** Connects the websocket. Blocks until connected, or throws if the connection times out. */
  public void connect() throws ConnectionTimeoutException {
    if (getState() == ReadyState.OPEN) {
      return;
    }

    if (!isKeepConnected()) {
      setKeepConnected(true);
      log.info("Connecting to " + options.getWebsocketApiUrl() + "...");
      executorService.scheduleAtFixedRate(
          () -> {
            if (isKeepConnected()) {
              if (getState() != ReadyState.OPEN) {
                final boolean isReconnect = !isWebsocketNull();
                log.info("Not connected. Attempting to " + (isReconnect ? "reconnect" : "connect"));
                if (isReconnect) {
                  this.getWebsocket().closeConnection(0, "");
                }
                initWebsocket();
                this.getWebsocket().connect();
              }
            } else {
              if (getState() != ReadyState.CLOSED) {
                log.info("Closing connection");
                getWebsocket().closeConnection(0, "");
                this.setWebsocket(null);
                executorService.shutdown();
              }
            }
          },
          0L,
          options.getReconnectRetryInterval(),
          TimeUnit.MILLISECONDS);
    }

    waitForState(ReadyState.OPEN);
    if (getState() != ReadyState.OPEN) {
      throw new ConnectionTimeoutException(options.getWebsocketApiUrl());
    }

    if (keyExchangeStream != null && keyExchangeSub == null) {
      keyExchangeSub =
          subscribe(
              keyExchangeStream,
              new MessageHandler() {
                @Override
                public void onMessage(Subscription sub, StreamMessage message) {
                  try {
                    if (message.getMessageType() == StreamMessage.MessageType.GROUP_KEY_REQUEST) {
                      try {
                        keyExchangeUtil.handleGroupKeyRequest(message);
                      } catch (Exception e) {
                        GroupKeyRequest groupKeyRequest =
                            (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(message);
                        StreamMessage errorMessage =
                            msgCreationUtil.createGroupKeyErrorResponse(
                                message.getPublisherId(), groupKeyRequest, e);
                        publish(errorMessage); // sending the error to the sender of 'message'
                      }
                    } else if (message.getMessageType()
                        == StreamMessage.MessageType.GROUP_KEY_RESPONSE) {
                      keyExchangeUtil.handleGroupKeyResponse(message);
                    } else if (message.getMessageType()
                        == StreamMessage.MessageType.GROUP_KEY_ANNOUNCE) {
                      keyExchangeUtil.handleGroupKeyAnnounce(message);
                    } else if (message.getMessageType()
                        == StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE) {
                      Map<String, Object> content = message.getParsedContent();
                      log.warn(
                          "Received error of type {} from {}: {}",
                          content.get("code"),
                          message.getPublisherId(),
                          content.get("message"));
                    } else {
                      throw new MalformedMessageException(
                          "Unexpected message type on key exchange stream: "
                              + message.getMessageType());
                    }
                  } catch (Exception e) {
                    log.error(e.getMessage());
                  }
                }
              });
    }
    log.info("Connected to " + options.getWebsocketApiUrl());
  }

  /** Disconnects the websocket. Blocks until disconnected, or throws if the operation times out. */
  public void disconnect() throws ConnectionTimeoutException {
    if (getState() == ReadyState.CLOSED) {
      return;
    }
    setKeepConnected(false);
    waitForState(ReadyState.CLOSED);
    ReadyState state = getState();
    if (state != ReadyState.CLOSED) {
      throw new RuntimeException(
          String.format("Failed to disconnect: never went from %s to CLOSED readyState", state));
    }
  }

  public void setErrorMessageHandler(ErrorMessageHandler errorMessageHandler) {
    this.errorMessageHandler = errorMessageHandler;
  }

  private void waitForState(ReadyState target) {
    long maxWaitTime =
        options.getReconnectRetryInterval() + options.getConnectionTimeoutMillis() + 500;
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
    log.trace(
        "[{}] >> {}", publisherId != null ? publisherId.toString().substring(0, 6) : null, message);
    if (!isWebsocketNull()) {
      this.getWebsocket().send(message.toJson());
    } else {
      log.warn("send: websocket is null, not sending message {}", message);
    }
  }

  private boolean isWebsocketNull() {
    websocketRLock.lock();
    try {
      return (this.getWebsocket() == null);
    } finally {
      websocketRLock.unlock();
    }
  }

  public ReadyState getState() {
    if (isWebsocketNull()) {
      return ReadyState.CLOSED;
    }
    return this.getWebsocket().getReadyState();
  }

  public Address getPublisherId() {
    return publisherId;
  }

  public GroupKeyStore getKeyStore() {
    return keyStore;
  }

  @Override
  public Stream createStream(final Stream stream) throws IOException {
    return restClient.createStream(stream);
  }

  @Override
  public Stream getStream(final String streamId) throws IOException {
    return restClient.getStream(streamId);
  }

  @Override
  public Stream getStreamByName(final String name) throws IOException, AmbiguousResultsException {
    return restClient.getStreamByName(name);
  }

  @Override
  public Permission grant(
      final Stream stream, final Permission.Operation operation, final String user)
      throws IOException {
    return restClient.grant(stream, operation, user);
  }

  @Override
  public Permission grantPublic(final Stream stream, final Permission.Operation operation)
      throws IOException {
    return restClient.grantPublic(stream, operation);
  }

  @Override
  public UserInfo getUserInfo() throws IOException {
    return restClient.getUserInfo();
  }

  @Override
  public List<String> getPublishers(final String streamId) throws IOException {
    return restClient.getPublishers(streamId);
  }

  @Override
  public boolean isPublisher(final String streamId, final Address address) throws IOException {
    return restClient.isPublisher(streamId, address);
  }

  @Override
  public boolean isPublisher(final String streamId, final String ethAddress) throws IOException {
    return restClient.isPublisher(streamId, ethAddress);
  }

  @Override
  public List<String> getSubscribers(final String streamId) throws IOException {
    return restClient.getSubscribers(streamId);
  }

  @Override
  public boolean isSubscriber(final String streamId, final Address address) throws IOException {
    return restClient.isSubscriber(streamId, address);
  }

  @Override
  public boolean isSubscriber(final String streamId, final String ethAddress) throws IOException {
    return restClient.isSubscriber(streamId, ethAddress);
  }

  public DataUnionClient dataUnionClient(String mainnetAdminPrvKey, String sidechainAdminPrvKey) {
    return new DataUnionClient(
        options.getMainnetRpcUrl(),
        options.getDataUnionMainnetFactoryAddress(),
        mainnetAdminPrvKey,
        options.getSidechainRpcUrl(),
        options.getDataUnionSidechainFactoryAddress(),
        sidechainAdminPrvKey);
  }

  @Override
  public void logout() throws IOException {
    restClient.logout();
  }

  @Override
  public void newLogin(final BigInteger privateKey) throws IOException {
    restClient.login(privateKey); // getSessionToken();
  }

  @Override
  public String getSessionToken() {
    return restClient.getSessionToken();
  }
  /*
   * Message handling
   */

  protected void handleMessage(String rawMessageAsString) {
    try {
      // Handle different message types
      ControlMessage message = ControlMessage.fromJson(rawMessageAsString);

      log.trace(
          "[{}] << {}",
          publisherId != null ? publisherId.toString().substring(0, 6) : null,
          message);

      if (message != null) {
        try {
          if (message.getType() == BroadcastMessage.TYPE) {
            BroadcastMessage msg = (BroadcastMessage) message;
            handleMessage(msg.getStreamMessage(), Subscription::handleRealTimeMessage);
          } else if (message.getType() == UnicastMessage.TYPE) {
            UnicastMessage msg = (UnicastMessage) message;
            handleMessage(msg.getStreamMessage(), Subscription::handleResentMessage);
          } else if (message.getType() == SubscribeResponse.TYPE) {
            handleSubscribeResponse((SubscribeResponse) message);
          } else if (message.getType() == UnsubscribeResponse.TYPE) {
            handleUnsubscribeResponse((UnsubscribeResponse) message);
          } else if (message.getType() == ResendResponseResending.TYPE) {
            handleResendResponseResending((ResendResponseResending) message);
          } else if (message.getType() == ResendResponseNoResend.TYPE) {
            handleResendResponseNoResend((ResendResponseNoResend) message);
          } else if (message.getType() == ResendResponseResent.TYPE) {
            handleResendResponseResent((ResendResponseResent) message);
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

  private void handleMessage(
      StreamMessage message, BiConsumer<Subscription, StreamMessage> subMsgHandler)
      throws SubscriptionNotFoundException {
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

  public void publish(
      Stream stream, Map<String, Object> payload, Date timestamp, GroupKey groupKey) {
    publish(stream, payload, timestamp, null, groupKey);
  }

  public void publish(
      Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
    publish(stream, payload, timestamp, partitionKey, null);
  }

  public void publish(
      Stream stream,
      Map<String, Object> payload,
      Date timestamp,
      @Nullable String partitionKey,
      @Nullable GroupKey newGroupKey) {
    // Convenience feature: allow user to call publish() without having had called connect()
    // beforehand.
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
    if (options.getEncryptionOptions().autoRevoke()
        && keyExchangeUtil.keyRevocationNeeded(stream.getId())) {
      keyExchangeUtil.rekey(stream.getId(), true);
    }

    StreamMessage streamMessage =
        msgCreationUtil.createStreamMessage(
            stream, payload, timestamp, partitionKey, currentKey, newGroupKey);
    try {
      publish(streamMessage);
    } catch (WebsocketNotConnectedException e) {
      // TODO: re-try sending once. Need to implement proper message enqueuing while not connected
      // later.
      connect();
      publish(streamMessage);
    }
  }

  private void publish(StreamMessage streamMessage) {
    send(new PublishRequest(newRequestId("pub"), streamMessage, restClient.getSessionToken()));
  }

  public GroupKey rekey(Stream stream) {
    return keyExchangeUtil.rekey(stream.getId(), false);
  }

  /*
   * Subscribe
   */
  public Subscription subscribe(Stream stream, MessageHandler handler) {
    final int partitions = stream.getPartitions();
    if (partitions > 1) {
      throw new PartitionNotSpecifiedException(stream.getId(), stream.getPartitions());
    }
    return subscribe(stream, 0, handler, null, false);
  }

  public Subscription subscribe(
      Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
    return subscribe(stream, partition, handler, resendOption, false);
  }

  protected Subscription subscribe(
      Stream stream,
      int partition,
      MessageHandler handler,
      ResendOption resendOption,
      boolean isExplicitResend) {
    if (!getState().equals(ReadyState.OPEN)) {
      connect();
    }

    SubscribeRequest subscribeRequest =
        new SubscribeRequest(
            newRequestId("sub"), stream.getId(), partition, restClient.getSessionToken());

    Subscription sub;
    BasicSubscription.GroupKeyRequestFunction requestFunction =
        (publisherId, groupKeyIds) -> sendGroupKeyRequest(stream.getId(), publisherId, groupKeyIds);
    if (resendOption == null) {
      sub =
          new RealTimeSubscription(
              stream.getId(),
              partition,
              handler,
              keyStore,
              keyExchangeUtil,
              requestFunction,
              options.getPropagationTimeout(),
              options.getResendTimeout(),
              options.getSkipGapsOnFullQueue());
    } else if (isExplicitResend) {
      sub =
          new HistoricalSubscription(
              stream.getId(),
              partition,
              handler,
              keyStore,
              keyExchangeUtil,
              resendOption,
              requestFunction,
              options.getPropagationTimeout(),
              options.getResendTimeout(),
              options.getSkipGapsOnFullQueue());
    } else {
      sub =
          new CombinedSubscription(
              stream.getId(),
              partition,
              handler,
              keyStore,
              keyExchangeUtil,
              resendOption,
              requestFunction,
              options.getPropagationTimeout(),
              options.getResendTimeout(),
              options.getSkipGapsOnFullQueue());
    }
    sub.setGapHandler(
        (MessageRef from, MessageRef to, Address publisherId, String msgChainId) -> {
          ResendRangeRequest req =
              new ResendRangeRequest(
                  newRequestId("resend"),
                  stream.getId(),
                  partition,
                  from,
                  to,
                  publisherId,
                  msgChainId,
                  restClient.getSessionToken());
          sub.setResending(true);
          send(req);
        });
    subs.add(sub);
    sub.setState(Subscription.State.SUBSCRIBING);
    send(subscribeRequest);
    return sub;
  }

  private void resubscribe(Subscription sub) {
    SubscribeRequest subscribeRequest =
        new SubscribeRequest(
            newRequestId("resub"),
            sub.getStreamId(),
            sub.getPartition(),
            restClient.getSessionToken());
    sub.setState(Subscription.State.SUBSCRIBING);
    send(subscribeRequest);
  }

  /*
   * Resend
   */

  public void resend(
      Stream stream, int partition, MessageHandler handler, ResendOption resendOption) {
    MessageHandler a =
        new MessageHandler() {
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
    UnsubscribeRequest unsubscribeRequest =
        new UnsubscribeRequest(newRequestId("unsub"), sub.getStreamId(), sub.getPartition());
    sub.setState(Subscription.State.UNSUBSCRIBING);
    sub.setResending(false);
    send(unsubscribeRequest);
  }

  @Override
  public void addStreamToStorageNode(final String streamId, final StorageNode storageNode)
      throws IOException {
    restClient.addStreamToStorageNode(streamId, storageNode);
  }

  @Override
  public void removeStreamToStorageNode(final String streamId, final StorageNode storageNode)
      throws IOException {
    restClient.removeStreamToStorageNode(streamId, storageNode);
  }

  @Override
  public List<StorageNode> getStorageNodes(final String streamId) throws IOException {
    return restClient.getStorageNodes(streamId);
  }

  @Override
  public List<StreamPart> getStreamPartsByStorageNode(final StorageNode storageNode)
      throws IOException {
    return restClient.getStreamPartsByStorageNode(storageNode);
  }

  private void handleSubscribeResponse(SubscribeResponse res) throws SubscriptionNotFoundException {
    Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
    sub.setState(Subscription.State.SUBSCRIBED);
    if (sub.hasResendOptions()) {
      ResendOption resendOption = sub.getResendOption();
      ControlMessage req =
          resendOption.toRequest(
              newRequestId("resend"),
              res.getStreamId(),
              res.getStreamPartition(),
              restClient.getSessionToken());
      send(req);
      OneTimeResend resend =
          new OneTimeResend(getWebsocket(), req, options.getResendTimeout(), sub);
      secondResends.put(sub.getId(), resend);
      resend.start();
    }
  }

  private void handleUnsubscribeResponse(UnsubscribeResponse res)
      throws SubscriptionNotFoundException {
    Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
    sub.setState(Subscription.State.UNSUBSCRIBED);
    subs.remove(sub);
  }

  private void handleResendResponseResending(ResendResponseResending res)
      throws SubscriptionNotFoundException {
    Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
    sub.startResend();
    log.debug("Resending started for subscription " + sub.getId());
  }

  private void handleResendResponseNoResend(ResendResponseNoResend res)
      throws SubscriptionNotFoundException {
    Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
    sub.endResend();
  }

  private void handleResendResponseResent(ResendResponseResent res)
      throws SubscriptionNotFoundException {
    Subscription sub = subs.get(res.getStreamId(), res.getStreamPartition());
    sub.endResend();
  }

  private void sendGroupKeyRequest(String streamId, Address publisherId, List<String> groupKeyIds) {
    if (!getState().equals(ReadyState.OPEN)) {
      connect();
    }
    StreamMessage request =
        msgCreationUtil.createGroupKeyRequest(
            publisherId, streamId, encryptionUtil.getPublicKeyAsPemString(), groupKeyIds);
    publish(request);
  }

  private String newRequestId(String prefix) {
    return String.format("%s.%s.%d", prefix, IdGenerator.get(), requestCounter++);
  }

  public boolean isKeepConnected() {
    boolean result;
    keepConnectedRLock.lock();
    try {
      result = this.keepConnected;
    } finally {
      keepConnectedRLock.unlock();
    }
    return result;
  }

  public void setKeepConnected(boolean keepConnected) {
    keepConnectedWLock.lock();
    try {
      this.keepConnected = keepConnected;
    } finally {
      keepConnectedWLock.unlock();
    }
  }

  public void setWebsocket(WebSocketClient websocket) {
    websocketWLock.lock();
    try {
      this.websocket = websocket;
    } finally {
      websocketWLock.unlock();
    }
  }

  public static class StreamrWebSocketClient extends WebSocketClient {
    private final Logger log = LoggerFactory.getLogger(StreamrWebSocketClient.class);
    private final StreamrClient streamrClient;

    public StreamrWebSocketClient(final StreamrClient streamrClient, final URI websocketApiUrl) {
      super(websocketApiUrl);
      this.streamrClient = streamrClient;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
      log.info("Connection established");
      streamrClient.onOpen();
      try {
        streamrClient.subs.forEach(streamrClient::resubscribe);
      } catch (WebsocketNotConnectedException e) {
        log.error("Failed to resubscribe", e);
      }
    }

    @Override
    public void onMessage(String message) {
      this.streamrClient.handleMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
      log.info("Connection closed! Code: " + code + ", Reason: " + reason);
      this.streamrClient.onClose();
    }

    @Override
    public void onError(Exception ex) {
      log.error("StreamrWebSocketClient#onError called", ex);
      if (!(ex instanceof IOException)) {
        this.streamrClient.onError(ex);
      }
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
      super.send(text);
    }
  }
}
