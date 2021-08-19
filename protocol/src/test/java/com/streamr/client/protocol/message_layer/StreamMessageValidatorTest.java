package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.options.SigningOptions;
import com.streamr.client.protocol.rest.FieldConfig;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.protocol.rest.StreamConfig;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.AddressValidityUtil;
import com.streamr.client.protocol.utils.EncryptionUtil;
import com.streamr.client.protocol.utils.GroupKey;
import com.streamr.client.protocol.utils.MessageCreationUtil;
import com.streamr.client.testing.TestingContentX;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMessageValidatorTest {
  private static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
  private final String publisherPrivateKey =
      "d462a6f2ccd995a346a841d110e8c6954930a1c22851c0032d3116d8ccd2296a";
  private final Address publisher = new Address("0x6807295093ac5da6fb2a10f7dedc5edd620804fb");
  private final String subscriberPrivateKey =
      "81fe39ed83c4ab997f64564d0c5a630e34c621ad9bbe51ad2754fac575fc0c46";
  private final Address subscriber = new Address("0xbe0ab87a1f5b09afe9101b09e3c86fd8f4162527");
  StreamMessageValidator validator;

  private final GroupKey groupKey = GroupKey.generate();
  private final EncryptionUtil encryptionUtil = new EncryptionUtil();

  private final MessageCreationUtil publisherMsgCreationUtil =
      new MessageCreationUtil(new BigInteger(publisherPrivateKey, 16), publisher);
  private final MessageCreationUtil subscriberMsgCreationUtil =
      new MessageCreationUtil(new BigInteger(subscriberPrivateKey, 16), subscriber);

  private StreamMessage groupKeyRequest;
  private StreamMessage msgSigned;
  private StreamMessage groupKeyResponse;
  private StreamMessage groupKeyAnnounceRekey;
  private StreamMessage groupKeyErrorResponse;

  private String signature =
      "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b";
  private MessageId msgId =
      new MessageId.Builder()
          .withStreamId("streamId")
          .withStreamPartition(0)
          .withTimestamp(425235315L)
          .withSequenceNumber(0L)
          .withPublisherId(PUBLISHER_ID)
          .withMsgChainId("msgChainId")
          .createMessageId();

  // The signature of this message is invalid but still in a correct format
  StreamMessage msgInvalid =
      new StreamMessage.Builder()
          .withMessageId(msgId)
          .withPreviousMessageRef(null)
          .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
          .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
          .withEncryptionType(StreamMessage.EncryptionType.NONE)
          .withGroupKeyId(null)
          .withSignatureType(StreamMessage.SignatureType.ETH)
          .withSignature(signature)
          .createStreamMessage();

  // By checking that this message is verified without throwing, we ensure that the SigningUtil is
  // not called because the signature is not in the correct form
  StreamMessage msgWrongFormat =
      new StreamMessage.Builder()
          .withMessageId(msgId)
          .withPreviousMessageRef(null)
          .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
          .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
          .withEncryptionType(StreamMessage.EncryptionType.NONE)
          .withGroupKeyId(null)
          .withSignatureType(StreamMessage.SignatureType.ETH)
          .withSignature("wrong-signature")
          .createStreamMessage();

  StreamMessage msgUnsigned =
      new StreamMessage.Builder()
          .withMessageId(msgId)
          .withPreviousMessageRef(null)
          .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
          .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
          .withEncryptionType(StreamMessage.EncryptionType.NONE)
          .withGroupKeyId(null)
          .withSignatureType(StreamMessage.SignatureType.NONE)
          .withSignature(null)
          .createStreamMessage();

  List<Address> publishers;
  List<Address> subscribers;
  Stream stream;

  AddressValidityUtil addressValidityUtil =
      new AddressValidityUtil(
          (String id) -> {
            return subscribers;
          },
          (String streamId, Address address) -> {
            return subscribers.contains(address);
          },
          (String id) -> {
            return publishers;
          },
          (String streamId, Address address) -> {
            return publishers.contains(address);
          });

  StreamMessageValidator getValidator(SigningOptions.SignatureVerificationPolicy verifySignatures) {
    return new StreamMessageValidator(
        (String id) -> {
          return stream;
        },
        addressValidityUtil,
        verifySignatures);
  }

  @BeforeEach
  void setup() {
    stream =
        new Stream.Builder()
            .withName("test-stream")
            .withDescription("")
            .withId("streamId")
            .withPartitions(1)
            .withConfig(new StreamConfig(new FieldConfig("field", FieldConfig.Type.STRING)))
            .withRequireSignedData(false)
            .withRequireEncryptedData(false)
            .createStream();

    msgSigned =
        StreamMessage.deserialize(
            "[31,[\"tagHE6nTQ9SJV2wPoCxBFw\",0,1587141844396,0,\"0x6807295093ac5da6fb2a10f7dedc5edd620804fb\",\"k000EDTMtqOTLM8sirFj\"],[1587141844312,0],27,0,\"{\\\"eventType\\\":\\\"trade\\\",\\\"eventTime\\\":1587141844398,\\\"symbol\\\":\\\"ETHBTC\\\",\\\"tradeId\\\":172530352,\\\"price\\\":0.02415,\\\"quantity\\\":0.296,\\\"buyerOrderId\\\":687544144,\\\"sellerOrderId\\\":687544104,\\\"time\\\":1587141844396,\\\"maker\\\":false,\\\"ignored\\\":true}\",2,\"0x6ad42041804c34902aaf7f07780b3e468ec2faec84eda2ff504d5fc26377d5556481d133d7f3f112c63cd48ee9081172013fb0ae1a61b45ee9ca89e057b099591b\"]");

    groupKeyRequest =
        subscriberMsgCreationUtil.createGroupKeyRequest(
            publisher,
            stream.getId(),
            encryptionUtil.getPublicKeyAsPemString(),
            Arrays.asList(groupKey.getGroupKeyId()));
    groupKeyResponse =
        publisherMsgCreationUtil.createGroupKeyResponse(
            subscriber,
            (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(groupKeyRequest),
            Arrays.asList(groupKey));
    groupKeyAnnounceRekey =
        publisherMsgCreationUtil.createGroupKeyAnnounce(
            subscriber,
            stream.getId(),
            encryptionUtil.getPublicKeyAsPemString(),
            Arrays.asList(groupKey));
    groupKeyErrorResponse =
        publisherMsgCreationUtil.createGroupKeyErrorResponse(
            subscriber,
            (GroupKeyRequest) AbstractGroupKeyMessage.fromStreamMessage(groupKeyRequest),
            new Exception("Test exception"));

    validator = getValidator(SigningOptions.SignatureVerificationPolicy.ALWAYS);
    publishers = new ArrayList<>();
    publishers.add(PUBLISHER_ID);
    publishers.add(publisher);

    subscribers = new ArrayList<>();
    subscribers.add(subscriber);
  }

  /*
   * Validating normal messages
   */

  @Test
  void passesValidationForValidSignatures() {
    validator.validate(msgSigned);
  }

  @Test
  void canOpenTheCachesAgainAfterTheyHaveBeenClosed() {
    validator.clearAndClose();
    validator.validate(msgSigned);
  }

  @Test
  void shouldReturnTrueWithoutVerifyingIfPolicyIsNeverForBothSignedAndUnsignedMessages() {
    validator = getValidator(SigningOptions.SignatureVerificationPolicy.NEVER);
    validator.validate(
        msgWrongFormat); // Signingvalidator.hasValidSignature() would throw if called
    validator.validate(msgUnsigned);
    validator.validate(msgSigned);
  }

  @Test
  void shouldThrowIfPolicyIsAlwaysAndMessageNotSigned() {
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(msgUnsigned);
            });
    assertEquals(ValidationException.Reason.POLICY_VIOLATION, e.getReason());
  }

  @Test
  void shouldThrowIfTheSignatureIsInvalid() {
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(msgInvalid);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void shouldVerifyIfPolicyIsAutoAndSignatureIsPresentOrEvenIfStreamDoesNotRequireSignedData() {
    validator = getValidator(SigningOptions.SignatureVerificationPolicy.AUTO);
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(msgInvalid);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void shouldThrowIfPolicyIsAutoOrSignatureIsNotPresentAndStreamRequiresSignedData() {
    stream = new Stream.Builder(stream).withRequireSignedData(true).createStream();
    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(msgUnsigned);
            });
    assertEquals(ValidationException.Reason.POLICY_VIOLATION, e.getReason());
  }

  @Test
  void acceptsValidEncryptedMessages() {
    stream = new Stream.Builder(stream).withRequireEncryptedData(true).createStream();
    msgSigned =
        new StreamMessage.Builder(msgSigned)
            .withEncryptionType(StreamMessage.EncryptionType.AES)
            .createStreamMessage();

    validator.validate(msgSigned);
  }

  @Test
  void rejectsUnencryptedMessagesIfEncryptionIsRequired() {
    stream = new Stream.Builder(stream).withRequireEncryptedData(true).createStream();
    msgSigned =
        new StreamMessage.Builder(msgSigned)
            .withEncryptionType(StreamMessage.EncryptionType.NONE)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(msgSigned);
            });
    assertEquals(ValidationException.Reason.POLICY_VIOLATION, e.getReason());
  }

  /* Validating GroupKeyRequests */

  @Test
  void requestAcceptsValid() {
    validator.validate(groupKeyRequest);
  }

  @Test
  void rejectsUnsigned() {
    groupKeyRequest =
        new StreamMessage.Builder(groupKeyRequest)
            .withSignature(null)
            .withSignatureType(StreamMessage.SignatureType.NONE)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyRequest);
            });
    assertEquals(ValidationException.Reason.UNSIGNED_NOT_ALLOWED, e.getReason());
  }

  @Test
  void rejectsInvalidSignatures() {
    String signature = groupKeyRequest.getSignature().replace('a', 'b');
    groupKeyRequest =
        new StreamMessage.Builder(groupKeyRequest)
            .withSignature(signature)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyRequest);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void rejectsMessagesToInvalidPublishers() {
    publishers.remove(publisher);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyRequest);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  @Test
  void rejectsMessagesFromUnpermittedSubscribers() {
    subscribers.remove(subscriber);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyRequest);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  /* Validating GroupKeyResponses */

  void groupKeyResponseAcceptsValid() {
    validator.validate(groupKeyResponse);
  }

  @Test
  void groupKeyResponseRejectsUnsigned() {
    groupKeyResponse =
        new StreamMessage.Builder(groupKeyResponse)
            .withSignature(null)
            .withSignatureType(StreamMessage.SignatureType.NONE)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyResponse);
            });
    assertEquals(ValidationException.Reason.UNSIGNED_NOT_ALLOWED, e.getReason());
  }

  @Test
  void groupKeyResponseRejectsInvalidSignatures() {
    String signature = groupKeyResponse.getSignature().replace('a', 'b');
    groupKeyResponse =
        new StreamMessage.Builder(groupKeyResponse)
            .withSignature(signature)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyResponse);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void groupKeyResponseRejectsMessagesFromInvalidPublishers() {
    publishers.remove(publisher);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyResponse);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  @Test
  void groupKeyResponseRejectsMessagesToUnpermittedSubscribers() {
    subscribers.remove(subscriber);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyResponse);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  /* Validating GroupKeyAnnounce */

  @Test
  void groupKeyAnnounceAcceptsValid() {
    validator.validate(groupKeyAnnounceRekey);
  }

  @Test
  void groupKeyAnnounceRejectsUnsigned() {
    groupKeyAnnounceRekey =
        new StreamMessage.Builder(groupKeyAnnounceRekey)
            .withSignature(null)
            .withSignatureType(StreamMessage.SignatureType.NONE)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyAnnounceRekey);
            });
    assertEquals(ValidationException.Reason.UNSIGNED_NOT_ALLOWED, e.getReason());
  }

  @Test
  void groupKeyAnnounceRejectsInvalidSignatures() {
    String signature = groupKeyAnnounceRekey.getSignature().replace('a', 'b');
    groupKeyAnnounceRekey =
        new StreamMessage.Builder(groupKeyAnnounceRekey)
            .withSignature(signature)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyAnnounceRekey);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void groupKeyAnnounceRejectsMessagesFromInvalidPublishers() {
    publishers.remove(publisher);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyAnnounceRekey);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  @Test
  void groupKeyAnnounceRejectsMessagesToUnpermittedSubscribers() {
    subscribers.remove(subscriber);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyAnnounceRekey);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  /* Validating GroupKeyErrorResponses */

  @Test
  void groupKeyErrorResponseAcceptsValid() {
    validator.validate(groupKeyErrorResponse);
  }

  @Test
  void groupKeyErrorResponseRejectsUnsigned() {
    groupKeyErrorResponse =
        new StreamMessage.Builder(groupKeyErrorResponse)
            .withSignature(null)
            .withSignatureType(StreamMessage.SignatureType.NONE)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyErrorResponse);
            });
    assertEquals(ValidationException.Reason.UNSIGNED_NOT_ALLOWED, e.getReason());
  }

  @Test
  void groupKeyErrorResponseRejectsInvalidSignatures() {
    String signature = groupKeyErrorResponse.getSignature().replace('a', 'b');
    groupKeyErrorResponse =
        new StreamMessage.Builder(groupKeyErrorResponse)
            .withSignature(signature)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyErrorResponse);
            });
    assertEquals(ValidationException.Reason.INVALID_SIGNATURE, e.getReason());
  }

  @Test
  void groupKeyErrorResponseRejectsMessagesFromInvalidPublishers() {
    publishers.remove(publisher);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyErrorResponse);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }

  @Test
  void groupKeyErrorResponseRejectsMessagesToUnpermittedSubscribers() {
    subscribers.remove(subscriber);

    ValidationException e =
        assertThrows(
            ValidationException.class,
            () -> {
              validator.validate(groupKeyErrorResponse);
            });
    assertEquals(ValidationException.Reason.PERMISSION_VIOLATION, e.getReason());
  }
}
