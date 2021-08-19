package com.streamr.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.MessageHandler;
import com.streamr.client.StreamrClient;
import com.streamr.client.options.ResendFromOption;
import com.streamr.client.options.ResendLastOption;
import com.streamr.client.options.ResendRangeOption;
import com.streamr.client.protocol.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.subs.Subscription;
import com.streamr.client.testing.TestingContent;
import com.streamr.client.testing.TestingKeys;
import com.streamr.client.testing.TestingStreamrClient;
import com.streamr.client.testing.TestingStreams;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.java_websocket.enums.ReadyState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamrWebsocketTest {
  private BigInteger publisherPrivateKey;
  private BigInteger subscriberPrivateKey;
  private StreamrClient publisher;
  private StreamrClient subscriber;
  private Stream stream;

  @BeforeEach
  void setup() throws IOException {
    publisherPrivateKey = TestingKeys.generatePrivateKey();
    subscriberPrivateKey = TestingKeys.generatePrivateKey();
    publisher = TestingStreamrClient.createClientWithPrivateKey(publisherPrivateKey);
    subscriber = TestingStreamrClient.createClientWithPrivateKey(subscriberPrivateKey);
    Stream proto =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamrWebsocketTest.class))
            .withName(TestingStreams.generateName(StreamrWebsocketTest.class))
            .withDescription("")
            .withRequireEncryptedData(false)
            .withRequireSignedData(false)
            .createStream();
    this.stream = publisher.createStream(proto);
    publisher.grant(
        this.stream, Permission.Operation.stream_get, subscriber.getPublisherId().toString());
    publisher.grant(
        this.stream, Permission.Operation.stream_subscribe, subscriber.getPublisherId().toString());
  }

  @AfterAll
  void cleanup() {
    if (publisher != null && publisher.getState() != ReadyState.CLOSED) {
      publisher.disconnect();
    }
    if (subscriber != null && subscriber.getState() != ReadyState.CLOSED) {
      subscriber.disconnect();
    }
  }

  @Test
  void clientCanConnectAndDisconnectOverWebsocket() {
    publisher.connect();
    assertEquals(ReadyState.OPEN, publisher.getState());
    publisher.disconnect();
    assertEquals(ReadyState.CLOSED, publisher.getState());
  }

  @Test
  void clientAutomaticallyConnectsForPublishing() throws IOException {
    Stream stream =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamrWebsocketTest.class))
            .withName(TestingStreams.generateName(StreamrWebsocketTest.class))
            .withDescription("")
            .createStream();
    stream = publisher.createStream(stream);
    Map<String, Object> map = TestingContent.mapWithValue("foo", "bar");
    publisher.publish(stream, map, new Date());
    assertEquals(ReadyState.OPEN, publisher.getState());
  }

  @Test
  void clientAutomaticallyConnectsForSubscribing() throws IOException {
    Stream stream =
        new Stream.Builder()
            .withId(TestingStreams.generateId(StreamrWebsocketTest.class))
            .withName(TestingStreams.generateName(StreamrWebsocketTest.class))
            .withDescription("")
            .createStream();
    stream = subscriber.createStream(stream);
    subscriber.subscribe(
        stream,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {}
        });
    assertEquals(ReadyState.OPEN, subscriber.getState());
  }

  @Disabled
  @Test
  void subscriberReceivesPublishedMmessages() throws InterruptedException {
    final int[] msgCount = {0};
    Subscription sub;
    // Subscribe to the stream
    final StreamMessage[] latestMsg = new StreamMessage[1];
    sub =
        subscriber.subscribe(
            stream,
            new MessageHandler() {
              @Override
              public void onMessage(Subscription s, StreamMessage message) {
                msgCount[0]++;
                latestMsg[0] = message;
              }
            });
    Thread.sleep(2000);
    // Produce messages to the stream
    for (int i = 1; i <= 10; i++) {
      publisher.publish(stream, TestingContent.mapWithValue("i", i));
      Thread.sleep(200);
    }

    // All messages have been received by subscriber
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (10 == msgCount[0]) {
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
    assertEquals(10, latestMsg[0].getParsedContent().get("i"));
    subscriber.unsubscribe(sub);
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (!sub.isSubscribed()) {
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
    publisher.publish(stream, TestingContent.mapWithValue("i", 11));
    pollingCondition.start();
    pollingCondition.join();
    // No more messages should be received, since we're unsubscribed
    assertEquals(10, msgCount[0]);
    assertFalse(sub.isSubscribed());
  }

  @Test
  void subscriberReceivesSignedMessageIfPublishedWithSignature() throws InterruptedException {
    // Subscribe to the stream
    final StreamMessage[] msg = new StreamMessage[1];
    subscriber.subscribe(
        stream,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            // reaching this point ensures that the signature verification didn't throw
            msg[0] = message;
          }
        });
    Thread.sleep(2000);
    publisher.publish(stream, TestingContent.mapWithValue("test", "signed"));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg[0] != null) {
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
    assertNotNull(msg[0]);
    assertEquals(publisher.getPublisherId(), msg[0].getPublisherId());
    assertEquals(StreamMessage.SignatureType.ETH, msg[0].getSignatureType());
    assertNotNull(msg[0].getSignature());
  }

  @Disabled
  @Test
  void subscriberCanDecryptMessagesWhenHeKnowsTheKeysUsedToEncrypt() throws InterruptedException {
    GroupKey key = GroupKey.generate();
    subscriber.getKeyStore().add(stream.getId(), key);
    // Subscribe to the stream
    final StreamMessage[] msg = new StreamMessage[1];
    subscriber.subscribe(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            // reaching this point ensures that the signature verification and decryption didn't
            // throw
            msg[0] = message;
          }
        },
        null);
    Thread.sleep(2000);
    final Map<String, Object> map = TestingContent.mapWithValue("test", "clear text");
    publisher.publish(stream, map, new Date(), null, key);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg[0] != null && msg[0].getParsedContent().equals(map)) {
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
    assertEquals(map, msg[0].getParsedContent());
    // publishing a second message with a new group key, triggers key rotate & announce
    final Map<String, Object> map1 = TestingContent.mapWithValue("test", "another clear text");
    publisher.publish(stream, map, new Date(), null, GroupKey.generate());
    // no need to explicitly give the new group key to the subscriber
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg[0] != null && msg[0].getParsedContent().equals(map1)) {
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
    assertEquals(map1, msg[0].getParsedContent());
  }

  @Disabled
  @Test
  void subscriberCanGetTheGroupKeyAndDecryptEncryptedMessagesUsingRsaKeyPair()
      throws InterruptedException {
    GroupKey key = GroupKey.generate();
    // Subscribe to the stream without knowing the group key
    final StreamMessage[] msg1 = {null};
    final StreamMessage[] msg2 = {null};
    subscriber.subscribe(
        stream,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            // reaching this point ensures that the signature verification and decryption didn't
            // throw
            if (msg1[0] == null) {
              msg1[0] = message;
            } else {
              msg2[0] = message;
            }
          }
        });
    Thread.sleep(2000);
    publisher.publish(
        stream, TestingContent.mapWithValue("test", "clear text"), new Date(), null, key);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg1[0] != null
                      && TestingContent.mapWithValue("test", "clear text")
                          .equals(msg1[0].getParsedContent())) {
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
    assertEquals(TestingContent.mapWithValue("test", "clear text"), msg1[0].getParsedContent());
    // publishing a second message with a new group key
    publisher.publish(
        stream,
        TestingContent.mapWithValue("test", "another clear text"),
        new Date(),
        null,
        GroupKey.generate());
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg2[0] != null
                      && TestingContent.mapWithValue("test", "another clear text")
                          .equals(msg2[0].getParsedContent())) {
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
        TestingContent.mapWithValue("test", "another clear text"), msg2[0].getParsedContent());
  }

  @Disabled
  @Test
  void subscriberCanGetTheNewGroupKeyAfterResetAndDecryptEncryptedMessages()
      throws InterruptedException {
    GroupKey key = GroupKey.generate();
    // Subscribe to the stream without knowing the group key
    final StreamMessage[] msg1 = {null};
    final StreamMessage[] msg2 = {null};
    subscriber.subscribe(
        stream,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            // reaching this point ensures that the signature verification and decryption didn't
            // throw
            if (msg1[0] == null) {
              msg1[0] = message;
            } else {
              msg2[0] = message;
            }
          }
        });
    Thread.sleep(2000);
    publisher.publish(
        stream, TestingContent.mapWithValue("test", "clear text"), new Date(), null, key);
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg1[0] != null) {
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
    // the subscriber got the group key and can decrypt
    assertEquals(TestingContent.mapWithValue("test", "clear text"), msg1[0].getParsedContent());
    // publishing a second message after a rekey to revoke old subscribers
    publisher.rekey(stream);
    publisher.publish(stream, TestingContent.mapWithValue("test", "another clear text"));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg1[0] != null) {
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
    // no need to explicitly give the new group key to the subscriber
    assertEquals(
        TestingContent.mapWithValue("test", "anbother clear text"), msg2[0].getParsedContent());
  }

  @Disabled
  @Test
  void subscriberCanGetTheHistoricalKeysAndDecryptOldEncryptedMessagesUsingRsaKeyPair()
      throws InterruptedException {
    // publishing historical messages with different group keys before subscribing
    List<GroupKey> keys = new ArrayList<>();
    keys.add(GroupKey.generate());
    keys.add(GroupKey.generate());
    publisher.publish(
        stream, TestingContent.mapWithValue("test", "clear text"), new Date(), null, keys.get(0));
    publisher.publish(
        stream,
        TestingContent.mapWithValue("test", "another clear text"),
        new Date(),
        null,
        keys.get(1));
    Thread.sleep(3000);
    // Subscribe to the stream with resend last without knowing the group keys
    final StreamMessage[] msg1 = {null};
    final StreamMessage[] msg2 = {null};
    final StreamMessage[] msg3 = {null};
    subscriber.subscribe(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            // reaching this point ensures that the signature verification and decryption didn't
            // throw
            if (msg1[0] == null) {
              msg1[0] = message;
            } else if (msg2[0] == null) {
              msg2[0] = message;
            } else if (msg3[0] == null) {
              msg3[0] = message;
            } else {
              throw new RuntimeException("Received unexpected message: " + message.serialize());
            }
          }
        },
        new ResendLastOption(3)); // need to resend 3 messages because the announce counts
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg1[0] != null && msg2[0] != null) {
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
    assertTrue(msg1[0] != null && msg2[0] != null);
    // the subscriber got the group keys and can decrypt the old messages
    assertEquals(TestingContent.mapWithValue("test", "clear text"), msg1[0].getParsedContent());
    assertEquals(
        TestingContent.mapWithValue("test", "another clear text"), msg2[0].getParsedContent());
    // The publisher publishes another message with latest key
    publisher.publish(
        stream, TestingContent.mapWithValue("test", "3"), new Date(), null, keys.get(1));
    pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (msg3[0] != null) {
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
    assertEquals(TestingContent.mapWithValue("test", "3"), msg3[0].getParsedContent());
  }

  @Disabled
  @Test
  void subscribeWithResendLast() throws InterruptedException {
    final boolean[] received = {false};
    publisher.publish(stream, TestingContent.mapWithValue("i", 1));
    Thread.sleep(6000); // wait to land in storage
    // Subscribe to the stream
    subscriber.subscribe(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            received[0] = message.getParsedContent() == TestingContent.mapWithValue("i", 1);
          }
        },
        new ResendLastOption(1));
    Thread.sleep(6000);
    assertTrue(received[0]);
  }

  @Disabled
  @Test
  void subscribeWithResendFrom() throws InterruptedException {
    final boolean[] received = {false};
    final boolean[] done = {false};
    publisher.publish(stream, TestingContent.mapWithValue("i", 1));
    Thread.sleep(2000);
    // Subscribe to the stream
    subscriber.subscribe(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            received[0] = message.getParsedContent() == TestingContent.mapWithValue("i", 1);
          }

          public void done(Subscription sub) {
            done[0] = true;
          }
        },
        new ResendFromOption(new Date(0)));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (done[0] && received[0]) {
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
    assertTrue(done[0] && received[0]);
  }

  @Disabled
  @Test
  void resendLast() throws InterruptedException {
    List<Map<String, Object>> receivedMsg = new ArrayList<>();
    final boolean[] done = {false};
    for (int i = 0; i <= 10; i++) {
      publisher.publish(stream, TestingContent.mapWithValue("i", i));
    }
    Thread.sleep(6000); // wait to land in storage
    // Resend last
    subscriber.resend(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            receivedMsg.add(message.getParsedContent());
          }

          public void done(Subscription sub) {
            done[0] = true;
          }
        },
        new ResendLastOption(5));
    List<Map<String, Object>> expectedMessages = new ArrayList<>();
    expectedMessages.add(Collections.singletonMap("i", 6.0));
    expectedMessages.add(Collections.singletonMap("i", 7.0));
    expectedMessages.add(Collections.singletonMap("i", 8.0));
    expectedMessages.add(Collections.singletonMap("i", 9.0));
    expectedMessages.add(Collections.singletonMap("i", 10.0));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (done[0] && expectedMessages.equals(receivedMsg)) {
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
    assertTrue(done[0]);
    assertEquals(expectedMessages, receivedMsg);
  }

  @Disabled
  @Test
  void resendFrom() throws InterruptedException {
    List<Map<String, Object>> receivedMsg = new ArrayList<>();
    final boolean[] done = {false};
    Date resendFromDate = null;
    for (int i = 0; i <= 10; i++) {
      publisher.publish(stream, TestingContent.mapWithValue("i", i));
      if (i == 7) {
        resendFromDate = new Date();
      }
    }
    Thread.sleep(6000); // wait to land in storage
    // Resend from
    subscriber.resend(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            receivedMsg.add(message.getParsedContent());
          }

          public void done(Subscription sub) {
            done[0] = true;
          }
        },
        new ResendFromOption(resendFromDate));

    List<Map<String, Object>> expectedMessages = new ArrayList<>();
    expectedMessages.add(Collections.singletonMap("i", 8.0));
    expectedMessages.add(Collections.singletonMap("i", 9.0));
    expectedMessages.add(Collections.singletonMap("i", 10.0));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (done[0] && expectedMessages.equals(receivedMsg)) {
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
    assertTrue(done[0]);
    assertEquals(expectedMessages, receivedMsg);
  }

  @Disabled
  @Test
  void resendRange() throws InterruptedException {
    List<Map<String, Object>> receivedMsg = new ArrayList<>();
    final boolean[] done = {false};
    Date resendFromDate = null;
    Date resendToDate = null;
    for (int i = 0; i <= 10; i++) {
      Date date = new Date();
      publisher.publish(stream, TestingContent.mapWithValue("i", i), date);
      if (i == 3) {
        resendFromDate = new Date(date.getTime() + 1);
      }
      if (i == 7) {
        resendToDate = new Date(date.getTime() - 1);
      }
    }
    Thread.sleep(6000); // wait to land in storage
    // Resend range
    subscriber.resend(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            receivedMsg.add(message.getParsedContent());
          }

          public void done(Subscription sub) {
            done[0] = true;
          }
        },
        new ResendRangeOption(resendFromDate, resendToDate));
    List<Map<String, Object>> expectedMessages = new ArrayList<>();
    expectedMessages.add(Collections.singletonMap("i", 4.0));
    expectedMessages.add(Collections.singletonMap("i", 5.0));
    expectedMessages.add(Collections.singletonMap("i", 6.0));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (done[0] && expectedMessages.equals(receivedMsg)) {
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
    assertTrue(done[0]);
    assertEquals(expectedMessages, receivedMsg);
  }

  @Disabled
  @Test
  void resendRangeAgain() throws InterruptedException {
    List<Map<String, Object>> receivedMsg = new ArrayList<>();
    final boolean[] done = {false};
    int j = 0;
    Date resendFromDate = null;
    Date resendToDate = null;
    for (j = 0; j < 2; j++) {
      for (int i = 0; i <= 10; i++) {
        Date date = new Date();
        publisher.publish(stream, TestingContent.mapWithValue("i", i), date);
        if (i == 3) {
          resendFromDate = new Date(date.getTime() + 1);
        }
        if (i == 7) {
          resendToDate = new Date(date.getTime() - 1);
        }
      }
      Thread.sleep(6000); // wait to land in storage
      // Resend range
      final int finalJ = j;
      subscriber.resend(
          stream,
          0,
          new MessageHandler() {
            @Override
            public void onMessage(Subscription s, StreamMessage message) {
              receivedMsg.add(message.getParsedContent());
            }

            public void done(Subscription sub) {
              if (finalJ == 1) {
                done[0] = true;
              }
            }
          },
          new ResendRangeOption(resendFromDate, resendToDate));
    }
    List<Map<String, Object>> expectedMessages = new ArrayList<>();
    expectedMessages.add(Collections.singletonMap("i", 4.0));
    expectedMessages.add(Collections.singletonMap("i", 5.0));
    expectedMessages.add(Collections.singletonMap("i", 6.0));
    expectedMessages.add(Collections.singletonMap("i", 4.0));
    expectedMessages.add(Collections.singletonMap("i", 5.0));
    expectedMessages.add(Collections.singletonMap("i", 6.0));
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (done[0] && expectedMessages.equals(receivedMsg)) {
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
    assertTrue(done[0]);
    assertEquals(expectedMessages, receivedMsg);
  }

  @Disabled
  @Test
  void subscribeWithResendLastWithKeyExchange() throws InterruptedException {
    boolean stop = false;
    final int[] publishedMessages = {0};
    final int[] receivedMessages = {0};
    final boolean finalStop = stop;
    Thread publisherThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                int i = 0;
                while (!finalStop) {
                  // The publisher generates a new key for every message
                  publishedMessages[0]++;
                  publisher.publish(
                      stream,
                      TestingContent.mapWithValue("i", i++),
                      new Date(),
                      "",
                      GroupKey.generate());
                  try {
                    Thread.sleep(500);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            });
    publisherThread.start();
    Thread.sleep(5000); // make sure some published messages have time to get written to storage

    // Subscribe with resend last
    subscriber.subscribe(
        stream,
        0,
        new MessageHandler() {
          @Override
          public void onMessage(Subscription s, StreamMessage message) {
            receivedMessages[0]++;
          }
        },
        new ResendLastOption(1000)); // resend all previous messages to make the counters match
    Thread.sleep(3000); // Time to do the key exchanges etc.
    stop = true;
    Thread pollingCondition =
        new Thread(
            new Runnable() {
              private final long timeout = 1000 * 10;

              @Override
              public void run() {
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() < timeout + start) {
                  if (!publisherThread.isAlive() && publishedMessages[0] == receivedMessages[0]) {
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
    assertFalse(publisherThread.isAlive());
    assertEquals(receivedMessages[0], publishedMessages[0]);
  }

  @Disabled
  @Test
  void twoInstancesOfSamePublisherPublishingToTheSameStream()
      throws IOException, InterruptedException {
    StreamrClient publisher2 = null;
    try {
      boolean stop = false;
      publisher2 = TestingStreamrClient.createClientWithPrivateKey(publisherPrivateKey);
      publisher.grant(
          stream, Permission.Operation.stream_get, publisher2.getPublisherId().toString());
      publisher.grant(
          stream, Permission.Operation.stream_publish, publisher2.getPublisherId().toString());
      GroupKey keyPublisher1 = GroupKey.generate();
      GroupKey keyPublisher2 = GroupKey.generate();
      final int[] publishedByPublisher1 = {0};
      final int[] publishedByPublisher2 = {0};
      final int[] receivedFromPublisher1 = {0};
      final int[] receivedFromPublisher2 = {0};
      final int[] unableToDecryptCount = {0};
      final boolean finalStop = stop;
      Thread publisher1Thread =
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  int i = 0;
                  while (!finalStop) {
                    // The publisher generates a new key for every message
                    publishedByPublisher1[0]++;
                    Map<String, Object> map = new HashMap<>();
                    map.put("publisher", 1);
                    map.put("i", i++);
                    publisher.publish(stream, map, new Date(), "", keyPublisher1);
                    try {
                      Thread.sleep(500);
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                  }
                }
              });
      publisher1Thread.start();
      final boolean finalStop1 = stop;
      final StreamrClient finalPublisher = publisher2;
      Thread publisher2Thread =
          new Thread(
              new Runnable() {
                @Override
                public void run() {
                  int i = 0;
                  while (!finalStop1) {
                    // The publisher generates a new key for every message
                    publishedByPublisher2[0]++;
                    Map<String, Object> map = new HashMap<>();
                    map.put("publisher", 2);
                    map.put("i", i++);
                    finalPublisher.publish(stream, map, new Date(), "", keyPublisher2);
                    try {
                      Thread.sleep(500);
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                    }
                  }
                }
              });
      publisher2Thread.start();
      Thread.sleep(5000); // make sure some published messages have time to get written to storage
      // Subscribe with resend last
      subscriber.subscribe(
          stream,
          0,
          new MessageHandler() {
            @Override
            public void onMessage(Subscription s, StreamMessage message) {
              if ((Integer) message.getParsedContent().get("publisher") == 1) {
                receivedFromPublisher1[0]++;
                log.info("Received from publisher1 message: {}", message.getParsedContent());
              } else if ((Integer) message.getParsedContent().get("publisher") == 2) {
                receivedFromPublisher2[0]++;
                log.info("Received from publisher2 message: {}", message.getParsedContent());
              } else {
                throw new RuntimeException(
                    "Received an unexpected message: " + message.getParsedContent());
              }
            }

            @Override
            public void onUnableToDecrypt(UnableToDecryptException e) {
              unableToDecryptCount[0]++;
            }
          },
          new ResendLastOption(1000)); // resend all previous messages to make the counters match
      Thread.sleep(3000); // Time to do the key exchanges etc.
      stop = true;
      Thread pollingCondition =
          new Thread(
              new Runnable() {
                private final long timeout = 1000 * 10;

                @Override
                public void run() {
                  long start = System.currentTimeMillis();
                  while (System.currentTimeMillis() < timeout + start) {
                    if (!publisher1Thread.isAlive()
                        && !publisher2Thread.isAlive()
                        && receivedFromPublisher1[0] == publishedByPublisher1[0]
                        && receivedFromPublisher2[0] == publishedByPublisher2[0]) {
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
      assertFalse(publisher1Thread.isAlive());
      assertFalse(publisher2Thread.isAlive());
      assertEquals(receivedFromPublisher1[0], publishedByPublisher1[0]);
      assertEquals(receivedFromPublisher2[0], publishedByPublisher2[0]);
      assertEquals(0, unableToDecryptCount[0]);
    } finally {
      publisher2.disconnect();
    }
  }
}
