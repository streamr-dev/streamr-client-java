package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.exceptions.InvalidGroupKeyRequestException;
import com.streamr.client.protocol.exceptions.SigningRequiredException;
import com.streamr.client.protocol.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage;
import com.streamr.client.protocol.message_layer.GroupKeyAnnounce;
import com.streamr.client.protocol.message_layer.GroupKeyErrorResponse;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.GroupKeyResponse;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.testing.TestingContentX;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessageCreationUtilTest {
  private static final Address SUBSCRIBER_ID =
      new Address("0x5555555555555555555555555555555555555555");
  private static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");

  private final BigInteger privateKey =
      new BigInteger("23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820", 16);
  private final Stream stream =
      new Stream.Builder()
          .withName("test-stream")
          .withDescription("")
          .withId("stream-id")
          .withPartitions(1)
          .withRequireSignedData(false)
          .withRequireEncryptedData(false)
          .createStream();
  private final MessageCreationUtil msgCreationUtil =
      new MessageCreationUtil(privateKey, PUBLISHER_ID);
  private final EncryptionUtil encryptionUtil = new EncryptionUtil();

  @Test
  void createStreamMessageCreatesStreamMessageWithCorrectValues() {
    Date timestamp = new Date();

    StreamMessage msg =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), timestamp);
    assertEquals(stream.getId(), msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(timestamp.getTime(), msg.getTimestamp());
    assertEquals(0L, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals(20, msg.getMsgChainId().length());
    assertEquals(null, msg.getPreviousMessageRef());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertEquals(TestingContentX.mapWithValue("foo", "bar"), msg.getParsedContent());
    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertNotNull(msg.getSignature());
  }

  @Test
  void createStreamMessageDoesntSignMessagesIfPrivateKeyIsNotDefined() {
    MessageCreationUtil msgCreationUtil2 = new MessageCreationUtil(null, PUBLISHER_ID);

    StreamMessage msg =
        msgCreationUtil2.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date());

    assertEquals(StreamMessage.SignatureType.NONE, msg.getSignatureType());
    assertEquals(null, msg.getSignature());
  }

  @Test
  void createStreamMessageEncryptsTheMessagesIfGroupKeyIsPassed() {
    GroupKey key = GroupKey.generate();

    StreamMessage msg =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date(), null, key, null);

    assertEquals(StreamMessage.EncryptionType.AES, msg.getEncryptionType());
    assertEquals(key.getGroupKeyId(), msg.getGroupKeyId());
  }

  @Test
  void createStreamMessageEncryptsNewKeyIfCurrentKeyAndNewKeyArePassed() throws Exception {
    GroupKey key = GroupKey.generate();
    GroupKey newKey = GroupKey.generate();

    StreamMessage msg =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date(), null, key, newKey);

    assertEquals(StreamMessage.EncryptionType.AES, msg.getEncryptionType());
    assertEquals(key.getGroupKeyId(), msg.getGroupKeyId());
    assertEquals(newKey, EncryptionUtil.decryptGroupKey(msg.getNewGroupKey(), key));
  }

  @Test
  void createStreamMessageWithDifferentTimestampsChainsMessagesWithSequenceNumberAlwaysZero() {
    long timestamp = new Date().getTime();

    StreamMessage msg1 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date(timestamp));
    StreamMessage msg2 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date(timestamp + 100));
    StreamMessage msg3 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), new Date(timestamp + 200));
    assertEquals(timestamp, msg1.getTimestamp());
    assertEquals(0L, msg1.getSequenceNumber());
    assertEquals(null, msg1.getPreviousMessageRef());
    assertEquals(timestamp + 100, msg2.getTimestamp());
    assertEquals(0L, msg2.getSequenceNumber());
    assertEquals(timestamp, msg2.getPreviousMessageRef().getTimestamp());
    assertEquals(0L, msg2.getPreviousMessageRef().getSequenceNumber());
    assertEquals(timestamp + 200, msg3.getTimestamp());
    assertEquals(0L, msg3.getSequenceNumber());
    assertEquals(timestamp + 100, msg3.getPreviousMessageRef().getTimestamp());
    assertEquals(0L, msg3.getPreviousMessageRef().getSequenceNumber());
  }

  @Test
  void createStreamMessageWithTheSameTimestampChainsMessagesWithIncreasingSequenceNumbers() {
    Date timestamp = new Date();

    StreamMessage msg1 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), timestamp);
    StreamMessage msg2 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), timestamp);
    StreamMessage msg3 =
        msgCreationUtil.createStreamMessage(
            stream, TestingContentX.mapWithValue("foo", "bar"), timestamp);
    assertEquals(timestamp.getTime(), msg1.getTimestamp());
    assertEquals(0L, msg1.getSequenceNumber());
    assertEquals(null, msg1.getPreviousMessageRef());
    assertEquals(timestamp.getTime(), msg2.getTimestamp());
    assertEquals(1L, msg2.getSequenceNumber());
    assertEquals(timestamp.getTime(), msg2.getPreviousMessageRef().getTimestamp());
    assertEquals(0L, msg2.getPreviousMessageRef().getSequenceNumber());
    assertEquals(timestamp.getTime(), msg3.getTimestamp());
    assertEquals(2L, msg3.getSequenceNumber());
    assertEquals(timestamp.getTime(), msg3.getPreviousMessageRef().getTimestamp());
    assertEquals(1L, msg3.getPreviousMessageRef().getSequenceNumber());
  }

  @Test
  void
      createStreamMessageWithSameTimestampsOnDifferentPartitionsChainsMessagesWithSequenceNumberAlwaysZero() {
    Date timestamp = new Date();
    Stream s = new Stream.Builder(stream).withPartitions(10).createStream();

    // Messages should go to different partitions
    StreamMessage msg1 =
        msgCreationUtil.createStreamMessage(
            s, TestingContentX.mapWithValue("foo", "bar"), timestamp, "partition-key-1");
    StreamMessage msg2 =
        msgCreationUtil.createStreamMessage(
            s, TestingContentX.mapWithValue("foo", "bar"), timestamp, "partition-key-2");

    assertNotEquals(msg2.getStreamPartition(), msg1.getStreamPartition());
    assertEquals(timestamp.getTime(), msg1.getTimestamp());
    assertEquals(0L, msg1.getSequenceNumber());
    assertEquals(null, msg1.getPreviousMessageRef());
    assertEquals(timestamp.getTime(), msg2.getTimestamp());
    assertEquals(0L, msg2.getSequenceNumber());
    assertEquals(null, msg2.getPreviousMessageRef());
  }

  @Test
  void createStreamMessageCorrectlyAssignsPartitionsBasedOnTheGivenPartitionKey() {

    Stream s = new Stream.Builder(stream).withPartitions(10).createStream();
    int[] partitions =
        new int[] {
          6, 7, 4, 4, 9, 1, 8, 0, 6, 6, 7, 6, 7, 3, 2, 2, 0, 9, 4, 9, 9, 5, 5, 1, 7, 3, 0, 6, 5, 6,
          3, 6, 3, 5, 6, 2, 3, 6, 7, 2, 1, 3, 2, 7, 1, 1, 5, 1, 4, 0, 1, 9, 7, 4, 2, 3, 2, 9, 7, 7,
          4, 3, 5, 4, 5, 3, 9, 0, 4, 8, 1, 7, 4, 8, 1, 2, 9, 9, 5, 3, 5, 0, 9, 4, 3, 9, 6, 7, 8, 6,
          4, 6, 0, 1, 1, 5, 8, 3, 9, 7
        };

    for (int i = 0; i < 100; i++) {
      StreamMessage msg =
          msgCreationUtil.createStreamMessage(
              s, TestingContentX.mapWithValue("foo", "bar"), new Date(), "key-" + i);
      assertEquals(partitions[i], msg.getStreamPartition());
    }
  }

  @Test
  void createGroupKeyRequestShouldThrowIfPrivateKeyIsNotSet() {
    MessageCreationUtil util = new MessageCreationUtil(null, SUBSCRIBER_ID);
    assertThrows(
        SigningRequiredException.class,
        () -> {
          util.createGroupKeyRequest(PUBLISHER_ID, "streamId", "", Arrays.asList("keyId1"));
        });
  }

  @Test
  void createGroupKeyRequestCreatesCorrectGroupKeyRequest() {
    MessageCreationUtil util = new MessageCreationUtil(privateKey, SUBSCRIBER_ID);

    StreamMessage msg =
        util.createGroupKeyRequest(
            PUBLISHER_ID, "streamId", "rsaPublicKey", Arrays.asList("keyId1"));
    GroupKeyRequest request =
        (GroupKeyRequest)
            AbstractGroupKeyMessage.deserialize(
                msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_REQUEST);

    assertEquals(KeyExchangeUtil.getKeyExchangeStreamId(PUBLISHER_ID), msg.getStreamId());
    assertEquals(SUBSCRIBER_ID, msg.getPublisherId());
    assertEquals(StreamMessage.MessageType.GROUP_KEY_REQUEST, msg.getMessageType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertNotNull(msg.getSignature());
    assertEquals("streamId", request.getStreamId());
    assertEquals("rsaPublicKey", request.getRsaPublicKey());
    assertEquals(Arrays.asList("keyId1"), request.getGroupKeyIds());
  }

  @Test
  void createGroupKeyResponseShouldThrowIfPrivateKeyIsNotSet() {
    MessageCreationUtil util = new MessageCreationUtil(null, PUBLISHER_ID);
    GroupKey key = GroupKey.generate();
    GroupKeyRequest request =
        new GroupKeyRequest(
            "requestId", "streamId", "publicKey", Arrays.asList(key.getGroupKeyId()));

    List<GroupKey> groupKeys = Arrays.asList(key);
    assertThrows(
        SigningRequiredException.class,
        () -> {
          util.createGroupKeyResponse(SUBSCRIBER_ID, request, groupKeys);
        });
  }

  @Test
  void createGroupKeyResponseCreatesCorrectGroupKeyResponse()
      throws InvalidGroupKeyException, UnableToDecryptException {
    GroupKey groupKey = GroupKey.generate();
    GroupKeyRequest request =
        new GroupKeyRequest(
            "requestId",
            "streamId",
            encryptionUtil.getPublicKeyAsPemString(),
            Arrays.asList(groupKey.getGroupKeyId()));

    StreamMessage msg =
        msgCreationUtil.createGroupKeyResponse(SUBSCRIBER_ID, request, Arrays.asList(groupKey));

    assertEquals(KeyExchangeUtil.getKeyExchangeStreamId(SUBSCRIBER_ID), msg.getStreamId());
    assertEquals(StreamMessage.MessageType.GROUP_KEY_RESPONSE, msg.getMessageType());
    assertEquals(StreamMessage.EncryptionType.RSA, msg.getEncryptionType());
    assertNotNull(msg.getSignature());

    GroupKeyResponse response =
        (GroupKeyResponse)
            AbstractGroupKeyMessage.deserialize(
                msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_RESPONSE);

    assertEquals("streamId", response.getStreamId());
    List<EncryptedGroupKey> keys = response.getKeys();
    EncryptedGroupKey encryptedGroupKey = keys.get(0);
    assertEquals(groupKey, encryptionUtil.decryptWithPrivateKey(encryptedGroupKey));
  }

  @Test
  void createGroupKeyAnnounceShouldThrowIfPrivateKeyIsNotSet() {
    MessageCreationUtil util = new MessageCreationUtil(null, PUBLISHER_ID);
    GroupKey key = GroupKey.generate();

    List<GroupKey> groupKeys = Arrays.asList(key);
    assertThrows(
        SigningRequiredException.class,
        () -> {
          util.createGroupKeyAnnounce(SUBSCRIBER_ID, "streamId", "publicKey", groupKeys);
        });
  }

  @Test
  void createGroupKeyAnnounceSendsTheGroupKeyAnnounceRsaEncryptedOnTheSubscribersKeyExchangeStream()
      throws InvalidGroupKeyException, UnableToDecryptException {
    GroupKey groupKey = GroupKey.generate();

    StreamMessage msg =
        msgCreationUtil.createGroupKeyAnnounce(
            SUBSCRIBER_ID,
            "streamId",
            encryptionUtil.getPublicKeyAsPemString(),
            Arrays.asList(groupKey));

    assertEquals(KeyExchangeUtil.getKeyExchangeStreamId(SUBSCRIBER_ID), msg.getStreamId());
    assertEquals(StreamMessage.MessageType.GROUP_KEY_ANNOUNCE, msg.getMessageType());
    assertEquals(StreamMessage.EncryptionType.RSA, msg.getEncryptionType());
    assertNotNull(msg.getSignature());

    GroupKeyAnnounce announce =
        (GroupKeyAnnounce)
            AbstractGroupKeyMessage.deserialize(
                msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_ANNOUNCE);

    assertEquals("streamId", announce.getStreamId());
    assertEquals(groupKey, encryptionUtil.decryptWithPrivateKey(announce.getKeys().get(0)));
  }

  @Test
  void createGroupKeyErrorResponseShouldThrowIfPrivateKeyIsNotSet() {
    MessageCreationUtil util = new MessageCreationUtil(null, PUBLISHER_ID);
    assertThrows(
        SigningRequiredException.class,
        () -> {
          util.createGroupKeyErrorResponse(
              SUBSCRIBER_ID,
              new GroupKeyRequest("requestId", "streamId", "rsaPublicKey", Arrays.asList("keyId1")),
              new Exception());
        });
  }

  @Test
  void createGroupKeyErrorResponseCreatesTheCorrectErrorMessage() {
    StreamMessage msg =
        msgCreationUtil.createGroupKeyErrorResponse(
            SUBSCRIBER_ID,
            new GroupKeyRequest("requestId", "streamId", "publicKey", Arrays.asList("keyId1")),
            new InvalidGroupKeyRequestException("some error message"));
    GroupKeyErrorResponse response =
        (GroupKeyErrorResponse)
            AbstractGroupKeyMessage.deserialize(
                msg.getSerializedContent(), StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE);
    assertEquals(KeyExchangeUtil.getKeyExchangeStreamId(SUBSCRIBER_ID), msg.getStreamId());
    assertEquals(StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE, msg.getMessageType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertNotNull(msg.getSignature());
    assertEquals("requestId", response.getRequestId());
    assertEquals("streamId", response.getStreamId());
    assertEquals("INVALID_GROUP_KEY_REQUEST", response.getCode());
    assertEquals("some error message", response.getMessage());
  }
}
