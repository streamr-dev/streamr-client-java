package com.streamr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.streamr.client.options.EncryptionOptions;
import com.streamr.client.options.ResendLastOption;
import com.streamr.client.options.ResendOption;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.control_layer.BroadcastMessage;
import com.streamr.client.protocol.control_layer.ControlMessage;
import com.streamr.client.protocol.control_layer.ErrorResponse;
import com.streamr.client.protocol.control_layer.PublishRequest;
import com.streamr.client.protocol.control_layer.ResendLastRequest;
import com.streamr.client.protocol.control_layer.ResendRangeRequest;
import com.streamr.client.protocol.control_layer.ResendResponseResent;
import com.streamr.client.protocol.control_layer.SubscribeRequest;
import com.streamr.client.protocol.control_layer.SubscribeResponse;
import com.streamr.client.protocol.control_layer.UnicastMessage;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.options.SigningOptions;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.EncryptionUtil;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.protocol.utils.KeyExchangeUtil;
import com.streamr.client.rest.ResourceNotFoundException;
import com.streamr.client.rest.StreamrRestClient;
import com.streamr.client.subs.Subscription;
import com.streamr.client.testing.TestWebSocketServer;
import com.streamr.client.testing.TestingAddresses;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingMeta;
import com.streamr.client.testing.TestingStreamrClient;
import com.streamr.client.utils.InMemoryGroupKeyStore;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamrClientTest {
  private TestWebSocketServer server = new TestWebSocketServer("localhost", 6000);
  private Stream stream;
  private TestingStreamrClient client;
  private int gapFillTimeout = 500;
  private int retryResendAfter = 500;

  @BeforeAll
  public void setupSpec() {
    server.start();
    stream =
        new Stream.Builder()
            .withName("")
            .withDescription("")
            .withId("test-stream")
            .withPartitions(1)
            .withRequireSignedData(false)
            .withRequireEncryptedData(false)
            .createStream();
  }

  @AfterAll
  public void cleanupSpec() throws IOException, InterruptedException {
    server.stop();
  }

  private void subscribeClient(ResendOption resendOption) throws InterruptedException {
    Subscription sub =
        client.subscribe(
            stream,
            0,
            new MessageHandler() {
              @Override
              public void onMessage(Subscription sub, StreamMessage message) {}
            },
            resendOption);

    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (1 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    System.out.println(server.getReceivedControlMessages());
    server.expect(
        new SubscribeRequest(
            server.getReceivedControlMessages().get(0).getMessage().getRequestId(),
            stream.getId(),
            0,
            client.getSessionToken()));

    client.receiveMessage(
        new SubscribeResponse(
            server.getReceivedControlMessages().get(0).getMessage().getRequestId(),
            stream.getId(),
            0));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (sub.isSubscribed()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertTrue(sub.isSubscribed());
  }

  private void subscribeClient() throws InterruptedException {
    subscribeClient(null);
  }

  @BeforeEach
  public void setup() throws InterruptedException {
    server.clear();
    final BigInteger privateKey =
        new BigInteger("d462a6f2ccd995a346a841d110e8c6954930a1c22851c0032d3116d8ccd2296a", 16);
    // Turn off autoRevoke, otherwise it will try and to REST API calls
    EncryptionOptions encryptionOptions =
        new EncryptionOptions(new InMemoryGroupKeyStore(), null, null, false);
    final StreamrClientOptions options =
        new StreamrClientOptions(
            SigningOptions.getDefault(),
            encryptionOptions,
            server.getWsUrl(),
            gapFillTimeout,
            retryResendAfter,
            false);
    options.setReconnectRetryInterval(1000);
    options.setConnectionTimeoutMillis(1000);

    final StreamrRestClient restClient =
        new StreamrRestClient.Builder()
            .withRestApiUrl(TestingMeta.REST_URL)
            .withPrivateKey(privateKey)
            .createStreamrRestClient();
    client =
        new TestingStreamrClient(options, restClient) {
          @Override
          public Stream getStream(String streamId) throws IOException, ResourceNotFoundException {
            return new Stream.Builder()
                .withName("default mock stream from TestingStreamrClient")
                .withDescription("")
                .withId(streamId)
                .withRequireSignedData(false)
                .withRequireEncryptedData(false)
                .createStream();
          }
        };
    client.connect();
    // TODO: client.login(privateKey)
    client.getSessionToken();

    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (1 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(1, server.getReceivedControlMessages().size());
    assertTrue(server.getReceivedControlMessages().get(0).getMessage() instanceof SubscribeRequest);
  }

  @AfterEach
  public void cleanup() {
    server.clear();
    client.disconnect();
  }

  private StreamMessage createMsg(
      String streamId,
      long timestamp,
      long sequenceNumber,
      Long prevTimestamp,
      Long prevSequenceNumber) {
    MessageId msgId =
        new MessageId.Builder()
            .withStreamId(streamId)
            .withStreamPartition(0)
            .withTimestamp(timestamp)
            .withSequenceNumber(sequenceNumber)
            .withPublisherId(TestingAddresses.PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    MessageRef prev = null;
    if (prevTimestamp != null) {
      prev = new MessageRef(prevTimestamp, prevSequenceNumber);
    }
    Map<String, Object> map = TestingContent.mapWithValue("hello", "world");
    return new StreamMessage.Builder()
        .withMessageId(msgId)
        .withPreviousMessageRef(prev)
        .withContent(TestingContent.fromJsonMap(map))
        .createStreamMessage();
  }

  @Disabled
  @Test
  void subscribeSendsSubscribeRequestAndOneResendLastRequestAfterSubscribeResponseIfAnswerReceived()
      throws InterruptedException {
    subscribeClient(new ResendLastOption(10));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    assertEquals(2, server.getReceivedControlMessages().size());
    server.expect(
        new ResendLastRequest(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0,
            10,
            client.getSessionToken()));
    client.receiveMessage(
        new UnicastMessage(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            createMsg("test-stream", 0, 0, null, null)));
    Thread.sleep(retryResendAfter + 200);
    server.noOtherMessagesReceived();
  }

  @Disabled
  @Test
  void subscribeSendsTwoResendLastRequestAfterSubscribeResponseIfNoAnswerReceived()
      throws InterruptedException {
    subscribeClient(new ResendLastOption(10));
    Thread.sleep(retryResendAfter + 200);

    assertEquals(3, server.getReceivedControlMessages().size());
    server.expect(
        new ResendLastRequest(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0,
            10,
            client.getSessionToken()));
    server.expect(
        new ResendLastRequest(
            server.getReceivedControlMessages().get(2).getMessage().getRequestId(),
            stream.getId(),
            0,
            10,
            client.getSessionToken()));
  }

  @Disabled
  @Test
  void requestsSingleResendIfGapIsDetectedAndThenFilled() throws InterruptedException {
    subscribeClient();
    client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 0, 0, null, null)));
    client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 2, 0, 1L, 0L)));
    Thread.sleep(gapFillTimeout);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }

              private final long timeout = 1000 * 3;
            });
    pollingCondition.start();
    pollingCondition.join();

    assertEquals(2, server.getReceivedControlMessages().size());
    server.expect(
        new ResendRangeRequest(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0,
            new MessageRef(0, 1),
            new MessageRef(1, 0),
            TestingAddresses.PUBLISHER_ID,
            "msgChainId",
            client.getSessionToken()));

    client.receiveMessage(
        new UnicastMessage(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            createMsg("test-stream", 1, 0, 0L, 0L)));
    client.receiveMessage(
        new ResendResponseResent(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0));

    Thread.sleep(gapFillTimeout + 200);
    server.noOtherMessagesReceived();
  }

  @Disabled
  @Test
  void requestsMultipleResendsIfGapIsDetectedAndNotFilled() throws InterruptedException {
    subscribeClient();
    client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 0, 0, null, null)));
    client.receiveMessage(new BroadcastMessage("", createMsg("test-stream", 2, 0, 1L, 0L)));
    Thread.sleep(2 * gapFillTimeout + 200);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (3 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(3, server.getReceivedControlMessages().size());
    server.expect(
        new ResendRangeRequest(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0,
            new MessageRef(0, 1),
            new MessageRef(1, 0),
            TestingAddresses.PUBLISHER_ID,
            "msgChainId",
            client.getSessionToken()));
    server.expect(
        new ResendRangeRequest(
            server.getReceivedControlMessages().get(2).getMessage().getRequestId(),
            stream.getId(),
            0,
            new MessageRef(0, 1),
            new MessageRef(1, 0),
            TestingAddresses.PUBLISHER_ID,
            "msgChainId",
            client.getSessionToken()));
  }

  @Disabled
  @Test
  void publishPublishesWithTheLatestKeyAddedToKeyStore() throws InterruptedException {
    GroupKey groupKey = GroupKey.generate();
    client.getKeyStore().add(stream.getId(), groupKey);
    client.publish(stream, TestingContent.mapWithValue("test", "secret"));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (1 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(1, server.getReceivedControlMessages().size());
    assertEquals(
        groupKey.getGroupKeyId(),
        ((PublishRequest) server.getReceivedControlMessages().get(0).getMessage())
            .getStreamMessage()
            .getGroupKeyId());
    assertTrue(
        !((PublishRequest) server.getReceivedControlMessages().get(0).getMessage())
            .getStreamMessage()
            .getSerializedContent()
            .contains("secret"));
  }

  @Disabled
  @Test
  void publishPublishesWithTheKeyGivenAsArgument() throws InterruptedException {
    GroupKey groupKey = GroupKey.generate();
    client.publish(
        stream, TestingContent.mapWithValue("test", "secret"), new Date(), null, groupKey);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (1 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(
        groupKey.getGroupKeyId(),
        ((PublishRequest) server.getReceivedControlMessages().get(0).getMessage())
            .getStreamMessage()
            .getGroupKeyId());
    assertTrue(
        !((PublishRequest) server.getReceivedControlMessages().get(0).getMessage())
            .getStreamMessage()
            .getSerializedContent()
            .contains("secret"));

    client.publish(stream, TestingContent.mapWithValue("test", "another"));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(
        groupKey.getGroupKeyId(),
        ((PublishRequest) server.getReceivedControlMessages().get(1).getMessage())
            .getStreamMessage()
            .getGroupKeyId());
    assertTrue(
        !((PublishRequest) server.getReceivedControlMessages().get(1).getMessage())
            .getStreamMessage()
            .getSerializedContent()
            .contains("another"));
  }

  @Disabled
  @Test
  void publishCalledWithTheCurrentGroupKeyDoesNotRotateTheKey() throws InterruptedException {
    GroupKey groupKey = GroupKey.generate();
    client.getKeyStore().add(stream.getId(), groupKey);
    client.publish(
        stream, TestingContent.mapWithValue("test", "secret"), new Date(), null, groupKey);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (1 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(
        groupKey.getGroupKeyId(),
        ((PublishRequest) server.getReceivedControlMessages().get(0).getMessage())
            .getStreamMessage()
            .getGroupKeyId());
  }

  @Disabled
  @Test
  void publishCalledWithNewGroupKeyRotatesTheKey() throws Exception {
    GroupKey currentKey = GroupKey.generate();
    GroupKey newKey = GroupKey.generate();
    client.getKeyStore().add(stream.getId(), currentKey);

    client.publish(stream, TestingContent.mapWithValue("test", "secret"), new Date(), null, newKey);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    List<TestWebSocketServer.ReceivedControlMessage> receivedControlMessages =
        server.getReceivedControlMessages();
    TestWebSocketServer.ReceivedControlMessage receivedControlMessage =
        receivedControlMessages.get(1);
    ControlMessage message = receivedControlMessage.getMessage();
    StreamMessage msg = null;
    if (message instanceof PublishRequest) {
      msg = ((PublishRequest) message).getStreamMessage();
    } else {
      fail("Expecting a PublishRequest, got: " + message.getClass());
    }
    // content is encrypted with current key
    assertEquals(currentKey.getGroupKeyId(), msg.getGroupKeyId());
    assertTrue(!msg.getSerializedContent().contains("secret"));
    // new key is encrypted with current key
    assertEquals(newKey.getGroupKeyId(), msg.getNewGroupKey().getGroupKeyId());
    assertEquals(newKey, EncryptionUtil.decryptGroupKey(msg.getNewGroupKey(), currentKey));

    client.publish(stream, TestingContent.mapWithValue("test", "secret"));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    // content is encrypted with new key
    StreamMessage msg2 =
        ((PublishRequest) server.getReceivedControlMessages().get(1).getMessage())
            .getStreamMessage();
    // content is encrypted with current key
    assertEquals(newKey.getGroupKeyId(), msg2.getGroupKeyId());
    assertFalse(msg2.getSerializedContent().contains("secret"));
    // there is no new key attached this time
    assertNull(msg2.getNewGroupKey());
  }

  @Test
  void clientReconnectsWhilePublishingIfServerIsTemporarilyDown() throws InterruptedException {
    Thread serverRestart =
        new Thread() {
          public void run() {
            try {
              server.stop();
            } catch (IOException e) {
              throw new RuntimeException(e);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            server = new TestWebSocketServer("localhost", 6000);
            server.start();
          }
        };
    client.publish(stream, TestingContent.mapWithValue("test", 1.0));
    client.publish(stream, TestingContent.mapWithValue("test", 2.0));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (2 == server.getReceivedControlMessages().size()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    serverRestart.start();
    Thread.sleep(200);
    client.publish(stream, TestingContent.mapWithValue("test", 3.0));
    client.publish(stream, TestingContent.mapWithValue("test", 4.0));
    client.publish(stream, TestingContent.mapWithValue("test", 5.0));
    client.publish(stream, TestingContent.mapWithValue("test", 6.0));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 3;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (4 == countReceivedPublishRequests()) {
                    break;
                  }
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    assertEquals(4, countReceivedPublishRequests());
  }

  private int countReceivedPublishRequests() {
    int counter = 0;
    for (TestWebSocketServer.ReceivedControlMessage msg : server.getReceivedControlMessages()) {
      if (msg.getMessage() instanceof PublishRequest) {
        counter++;
      }
    }
    return counter;
  }

  @Test
  void callingPublishFromMultipleThreadsDuringServerDisconnectDoesNotCauseErrors_CORE_1912()
      throws InterruptedException {
    Thread serverRestart =
        new Thread() {
          public void run() {
            try {
              server.stop();
            } catch (IOException e) {
              throw new RuntimeException(e);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            server = new TestWebSocketServer("localhost", 6000);
            server.start();
          }
        };

    final List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());
    final List<Thread> threads = new ArrayList<Thread>();
    for (int i = 4; i < 100; i++) {
      final double value = i;
      threads.add(
          new Thread() {
            public void run() {
              try {
                client.publish(stream, TestingContent.mapWithValue("test", value));
              } catch (Exception e) {
                errors.add(e);
              }
            }
          });
    }

    client.publish(stream, TestingContent.mapWithValue("test", 1.0));
    client.publish(stream, TestingContent.mapWithValue("test", 2.0));
    client.publish(stream, TestingContent.mapWithValue("test", 3.0));
    serverRestart.start();
    Thread.sleep(2000);
    for (int i = 0; i < threads.size(); i++) {
      threads.get(i).start();
    }
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 60;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  for (Thread t : threads) {
                    if (t.isAlive()) {
                      continue;
                    }
                  }
                  break;
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(Collections.emptyList(), errors);
  }

  @Disabled
  @Test
  void subscribedClientReconnectsIfServerIsTemporarilyDown() throws InterruptedException {
    Thread serverRestart =
        new Thread() {
          public void run() {
            try {
              server.stop();
            } catch (IOException e) {
              throw new RuntimeException(e);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            server = new TestWebSocketServer("localhost", 6000);
            server.start();
          }
        };
    subscribeClient();
    server.broadcastMessageToAll(stream, Collections.singletonMap("key", "msg #1"));

    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 60;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  if (1 == client.getReceivedStreamMessages().size()) {
                    break;
                  }
                }
              }
            });

    serverRestart.start();
    Thread.sleep(2000);
    pollingCondition.start();
    pollingCondition.join();
    assertEquals(1, client.getReceivedStreamMessages().size());

    server.expect(
        new SubscribeRequest(
            server.getReceivedControlMessages().get(0).getMessage().getRequestId(),
            KeyExchangeUtil.getKeyExchangeStreamId(
                new Address("0x6807295093ac5da6fb2a10f7dedc5edd620804fb")),
            0,
            client.getSessionToken()));
    server.expect(
        new SubscribeRequest(
            server.getReceivedControlMessages().get(1).getMessage().getRequestId(),
            stream.getId(),
            0,
            client.getSessionToken()));

    server.respondTo(server.getReceivedControlMessages().get(1));
    server.broadcastMessageToAll(stream, TestingContent.mapWithValue("key", "msg #2"));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 60;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  if (2 == client.getReceivedStreamMessages().size()) {
                    break;
                  }
                }
              }
            });
    pollingCondition.start();
    pollingCondition.join();

    assertEquals(2, client.getReceivedStreamMessages().size());
  }

  @Test
  void errorMessageHandlerIsCalled() {
    final boolean[] errorIsHandled = new boolean[1];
    client.setErrorMessageHandler(
        new ErrorMessageHandler() {
          @Override
          public void onErrorMessage(final ErrorResponse error) {
            errorIsHandled[0] = true;
          }
        });
    ErrorResponse err = new ErrorResponse("requestId", "error occurred", "TEST_ERROR");
    client.handleMessage(err.toJson());

    assertTrue(errorIsHandled[0]);
  }
}
