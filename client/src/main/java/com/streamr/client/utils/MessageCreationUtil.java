package com.streamr.client.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.streamr.client.crypto.KeysRsa;
import com.streamr.client.crypto.MD5;
import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.GroupKeyAnnounce;
import com.streamr.client.protocol.message_layer.GroupKeyErrorResponse;
import com.streamr.client.protocol.message_layer.GroupKeyRequest;
import com.streamr.client.protocol.message_layer.GroupKeyResponse;
import com.streamr.client.protocol.message_layer.MalformedMessageException;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StringOrMillisDateJsonAdapter;
import com.streamr.client.rest.Stream;
import com.streamr.client.stream.EncryptedGroupKey;
import com.streamr.client.stream.EncryptionUtil;
import com.streamr.client.stream.GroupKey;
import com.streamr.client.stream.InvalidGroupKeyException;
import com.streamr.client.stream.InvalidGroupKeyRequestException;
import com.streamr.client.stream.InvalidGroupKeyResponseException;
import com.streamr.client.stream.KeyExchangeUtil;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A stateful helper class to create StreamMessages, with the following responsibilities:
 *
 * <ul>
 *   <li>Maintains message chains by creating appropriate MessageIds and MessageRefs
 *   <li>Encrypts created messages
 *   <li>Signs created messages
 * </ul>
 */
public class MessageCreationUtil {
  private final BigInteger privateKey;
  private final Address publisherId;
  private final String msgChainId;
  private final Map<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

  public MessageCreationUtil(final BigInteger privateKey, final Address publisherId) {
    this.privateKey = privateKey;
    this.publisherId = publisherId;
    this.msgChainId = RandomStringUtils.randomAlphanumeric(20);
  }

  public StreamMessage createStreamMessage(
      Stream stream,
      Map<String, Object> payload,
      Date timestamp,
      @Nullable String partitionKey,
      @Nullable GroupKey groupKey,
      @Nullable GroupKey newGroupKey) {
    int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);

    Pair<MessageId, MessageRef> pair =
        createMsgIdAndRef(stream.getId(), streamPartition, timestamp.getTime());
    final String jsonMessage =
        new Moshi.Builder()
            .add(Date.class, new StringOrMillisDateJsonAdapter().nullSafe())
            .build()
            .adapter(Types.newParameterizedType(Map.class, String.class, Object.class))
            .toJson(payload);
    StreamMessage streamMessage =
        new StreamMessage.Builder()
            .withMessageId(pair.getLeft())
            .withPreviousMessageRef(pair.getRight())
            .withContent(StreamMessage.Content.Factory.withJsonAsPayload(jsonMessage))
            .createStreamMessage();

    // Encrypt content if the GroupKey is provided
    if (groupKey != null) {
      try {
        streamMessage = EncryptionUtil.encryptStreamMessage(streamMessage, groupKey);
      } catch (InvalidGroupKeyException e) {
        throw new RuntimeException(e);
      }

      // Encrypt and attach newGroupKey if it's provided
      if (newGroupKey != null) {
        final EncryptedGroupKey newGroup = EncryptionUtil.encryptGroupKey(newGroupKey, groupKey);
        streamMessage =
            new StreamMessage.Builder(streamMessage)
                .withNewGroupKey(newGroup)
                .createStreamMessage();
      }
    }

    if (privateKey != null) {
      streamMessage = SigningUtil.signStreamMessage(privateKey, streamMessage);
      // TODO: streamMessage.sign(privateKey);
    }
    return streamMessage;
  }

  public StreamMessage createGroupKeyRequest(
      Address publisherAddress, String streamId, String rsaPublicKey, List<String> groupKeyIds) {
    if (privateKey == null) {
      throw new SigningRequiredException(
          "Cannot create unsigned group key request. Must authenticate with an Ethereum account");
    }

    GroupKeyRequest request =
        new GroupKeyRequest(UUID.randomUUID().toString(), streamId, rsaPublicKey, groupKeyIds);

    String keyExchangeStreamId = KeyExchangeUtil.getKeyExchangeStreamId(publisherAddress);
    Pair<MessageId, MessageRef> pair = createDefaultMsgIdAndRef(keyExchangeStreamId);
    StreamMessage streamMessage =
        request.toStreamMessageBuilder(pair.getLeft(), pair.getRight()).createStreamMessage();

    // Never encrypt but always sign
    streamMessage = SigningUtil.signStreamMessage(privateKey, streamMessage);
    return streamMessage;
  }

  public StreamMessage createGroupKeyResponse(
      Address subscriberAddress, GroupKeyRequest request, List<GroupKey> groupKeys) {
    if (privateKey == null) {
      throw new SigningRequiredException(
          "Cannot create unsigned group key response. Must authenticate with an Ethereum account");
    }

    // Encrypt the group keys
    List<EncryptedGroupKey> encryptedGroupKeys =
        groupKeys.stream()
            .map(
                key -> {
                  RSAPublicKey publicKey =
                      KeysRsa.getPublicKeyFromString(request.getRsaPublicKey());
                  return EncryptionUtil.encryptWithPublicKey(key, publicKey);
                })
            .collect(Collectors.toList());

    GroupKeyResponse response =
        new GroupKeyResponse(request.getRequestId(), request.getStreamId(), encryptedGroupKeys);

    String keyExchangeStreamId = KeyExchangeUtil.getKeyExchangeStreamId(subscriberAddress);
    Pair<MessageId, MessageRef> pair = createDefaultMsgIdAndRef(keyExchangeStreamId);
    StreamMessage streamMessage =
        response
            .toStreamMessageBuilder(pair.getLeft(), pair.getRight())
            .withEncryptionType(StreamMessage.EncryptionType.RSA)
            .withGroupKeyId(request.getRsaPublicKey())
            .createStreamMessage();

    // Always sign
    streamMessage = SigningUtil.signStreamMessage(privateKey, streamMessage);
    return streamMessage;
  }

  public StreamMessage createGroupKeyAnnounce(
      Address subscriberAddress, String streamId, String publicKey, List<GroupKey> groupKeys) {
    if (privateKey == null) {
      throw new SigningRequiredException(
          "Cannot create unsigned group key announce. Must authenticate with an Ethereum account");
    }

    // Encrypt the group keys
    List<EncryptedGroupKey> encryptedGroupKeys =
        groupKeys.stream()
            .map(
                key -> {
                  RSAPublicKey rsaPublicKey = KeysRsa.getPublicKeyFromString(publicKey);
                  return EncryptionUtil.encryptWithPublicKey(key, rsaPublicKey);
                })
            .collect(Collectors.toList());

    GroupKeyAnnounce announce = new GroupKeyAnnounce(streamId, encryptedGroupKeys);

    String keyExchangeStreamId = KeyExchangeUtil.getKeyExchangeStreamId(subscriberAddress);
    Pair<MessageId, MessageRef> pair = createDefaultMsgIdAndRef(keyExchangeStreamId);
    StreamMessage streamMessage =
        announce
            .toStreamMessageBuilder(pair.getLeft(), pair.getRight())
            .withEncryptionType(StreamMessage.EncryptionType.RSA)
            .withGroupKeyId(publicKey)
            .createStreamMessage();

    // Always sign
    streamMessage = SigningUtil.signStreamMessage(privateKey, streamMessage);
    return streamMessage;
  }

  public StreamMessage createGroupKeyErrorResponse(
      Address destinationAddress, GroupKeyRequest request, Exception e) {
    if (privateKey == null) {
      throw new SigningRequiredException(
          "Cannot create unsigned error message. Must authenticate with an Ethereum account");
    }

    GroupKeyErrorResponse response =
        new GroupKeyErrorResponse(
            request.getRequestId(),
            request.getStreamId(),
            getErrorCodeFromException(e),
            e.getMessage(),
            request.getGroupKeyIds());

    String keyExchangeStreamId = KeyExchangeUtil.getKeyExchangeStreamId(destinationAddress);
    Pair<MessageId, MessageRef> pair = createDefaultMsgIdAndRef(keyExchangeStreamId);
    StreamMessage streamMessage =
        response.toStreamMessageBuilder(pair.getLeft(), pair.getRight()).createStreamMessage();

    // Never encrypt but always sign
    streamMessage = SigningUtil.signStreamMessage(privateKey, streamMessage);
    return streamMessage;
  }

  private String getErrorCodeFromException(Exception e) {
    if (e instanceof InvalidGroupKeyRequestException) {
      return "INVALID_GROUP_KEY_REQUEST";
    } else if (e instanceof InvalidGroupKeyResponseException) {
      return "INVALID_GROUP_KEY_RESPONSE";
    } else if (e instanceof MalformedMessageException) {
      return "INVALID_CONTENT_TYPE";
    } else {
      return "UNEXPECTED_ERROR";
    }
  }

  private int hash(String partitionKey) {
    byte[] bytes = MD5.digest(partitionKey.getBytes(StandardCharsets.UTF_8));
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  private int getStreamPartition(int nbPartitions, String partitionKey) {
    if (nbPartitions == 0) {
      throw new IllegalArgumentException("partitionCount is zero!");
    } else if (nbPartitions == 1) {
      return 0;
    } else if (partitionKey != null) {
      int h = hash(partitionKey);
      return Math.abs(h) % nbPartitions;
    } else {
      return (int) Math.floor(Math.random() * nbPartitions);
    }
  }

  private Pair<MessageId, MessageRef> createMsgIdAndRef(
      String streamId, int streamPartition, long timestamp) {
    String key = streamId + streamPartition;
    long sequenceNumber = getNextSequenceNumber(key, timestamp);
    MessageId msgId =
        new MessageId.Builder()
            .withStreamId(streamId)
            .withStreamPartition(streamPartition)
            .withTimestamp(timestamp)
            .withSequenceNumber(sequenceNumber)
            .withPublisherId(publisherId)
            .withMsgChainId(msgChainId)
            .createMessageId();
    MessageRef prevMsgRef = refsPerStreamAndPartition.get(key);
    Pair<MessageId, MessageRef> p = Pair.of(msgId, prevMsgRef);
    refsPerStreamAndPartition.put(key, new MessageRef(timestamp, sequenceNumber));
    return p;
  }

  private Pair<MessageId, MessageRef> createDefaultMsgIdAndRef(String streamId) {
    return createMsgIdAndRef(streamId, 0, (new Date()).getTime());
  }

  private long getNextSequenceNumber(String key, long timestamp) {
    MessageRef prev = refsPerStreamAndPartition.get(key);
    if (prev == null || prev.getTimestamp() != timestamp) {
      return 0L;
    }
    return prev.getSequenceNumber() + 1L;
  }
}
