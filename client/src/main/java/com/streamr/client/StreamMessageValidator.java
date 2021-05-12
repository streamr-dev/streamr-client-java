package com.streamr.client;

import com.streamr.client.options.SigningOptions.SignatureVerificationPolicy;
import com.streamr.client.protocol.message_layer.AbstractGroupKeyMessage;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.ValidationException;
import com.streamr.client.rest.Stream;
import com.streamr.ethereum.common.Address;
import com.streamr.client.utils.AddressValidityUtil;
import com.streamr.client.stream.KeyExchangeUtil;
import com.streamr.client.utils.SigningUtil;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

public class StreamMessageValidator {
  private static final int STREAM_EXPIRATION = 15;
  private Cache<String, Stream> streamsPerStreamId =
      new Cache2kBuilder<String, Stream>() {}.expireAfterWrite(STREAM_EXPIRATION, TimeUnit.MINUTES)
          .build();
  private final Function<String, Stream> getStreamFunction;
  private final AddressValidityUtil addressValidityUtil;
  private final SignatureVerificationPolicy signatureVerificationPolicy;

  public StreamMessageValidator(
      Function<String, Stream> getStreamFunction,
      AddressValidityUtil addressValidityUtil,
      SignatureVerificationPolicy signatureVerificationPolicy) {
    this.getStreamFunction = getStreamFunction;
    this.addressValidityUtil = addressValidityUtil;
    this.signatureVerificationPolicy = signatureVerificationPolicy;
  }

  private Cache<String, Stream> safeGetStreamCache() {
    if (streamsPerStreamId.isClosed()) {
      streamsPerStreamId =
          new Cache2kBuilder<String, Stream>() {}.expireAfterWrite(
                  STREAM_EXPIRATION, TimeUnit.MINUTES)
              .build();
    }
    return streamsPerStreamId;
  }

  /** Validates the message using the protocol rules and throws if the message fails validation. */
  public void validate(StreamMessage msg) throws ValidationException {
    if (msg == null) {
      throw new IllegalArgumentException("StreamMessage was null!");
    }

    switch (msg.getMessageType()) {
      case STREAM_MESSAGE:
        validateStreamMessage(msg);
        break;
      case GROUP_KEY_REQUEST:
        validateGroupKeyRequest(msg);
        break;
      case GROUP_KEY_ANNOUNCE:
        validateGroupKeyAnnounce(msg);
        break;
      case GROUP_KEY_RESPONSE:
      case GROUP_KEY_ERROR_RESPONSE:
        validateGroupKeyResponse(msg);
        break;
      default:
        throw ValidationException.Factory.create(
            ValidationException.Reason.INVALID_MESSAGE, msg);
    }
  }

  private void validateStreamMessage(StreamMessage msg) {
    Stream stream = getStream(msg.getStreamId());

    // Checks against stream metadata
    if (stream.requiresSignedData() && msg.getSignature() == null) {
      throw ValidationException.Factory.create(
          "Stream " + stream.getId() + " requires messages to be signed.",
          ValidationException.Reason.POLICY_VIOLATION, msg
      );
    }
    if (stream.requiresEncryptedData()
        && msg.getEncryptionType() == StreamMessage.EncryptionType.NONE) {
      throw ValidationException.Factory.create(
          "Stream " + stream.getId() + " requires messages to be encrypted.",
          ValidationException.Reason.POLICY_VIOLATION, msg
      );
    }
    if (msg.getStreamPartition() < 0 || msg.getStreamPartition() >= stream.getPartitions()) {
      throw ValidationException.Factory.create(
          "Partition "
              + msg.getStreamPartition()
              + " is out of range (0.."
              + (stream.getPartitions() - 1)
              + ")",
          ValidationException.Reason.INVALID_MESSAGE, msg
      );
    }

    assertValidSignatureAccordingToPolicy(msg);

    // Check publisher. Note that this can only be checked on signed messages.
    if (msg.getSignature() != null) {
      Address sender = msg.getPublisherId();

      // Check that the sender of the message is a valid publisher of the stream
      if (!addressValidityUtil.isValidPublisher(msg.getStreamId(), sender)) {
        throw ValidationException.Factory.create(
            sender + " is not a publisher on stream " + msg.getStreamId(),
            ValidationException.Reason.PERMISSION_VIOLATION, msg
        );
      }
    }
  }

  private void assertValidSignature(StreamMessage msg) throws ValidationException {
    boolean valid;
    try {
      valid = SigningUtil.hasValidSignature(msg);
    } catch (RuntimeException e) {
      throw ValidationException.Factory.create(
          e.getMessage(), ValidationException.Reason.INVALID_SIGNATURE, msg);
    }
    if (!valid) {
      throw ValidationException.Factory.create(
          ValidationException.Reason.INVALID_SIGNATURE, msg);
    }
  }

  private void assertValidSignatureAccordingToPolicy(StreamMessage msg) throws ValidationException {
    if (signatureVerificationPolicy == SignatureVerificationPolicy.ALWAYS
        && msg.getSignature() == null) {
      throw ValidationException.Factory.create(
          ValidationException.Reason.POLICY_VIOLATION, msg);
    }

    if (msg.getSignature() != null) {
      assertValidSignature(msg);
    }
  }

  private void validateGroupKeyRequest(StreamMessage streamMessage) {
    if (streamMessage.getSignature() == null) {
      throw ValidationException.Factory.create(
          ValidationException.Reason.UNSIGNED_NOT_ALLOWED, streamMessage);
    }

    assertKeyExchangeStream(streamMessage);

    // Signatures on key exchange messages are checked regardless of policy setting
    assertValidSignature(streamMessage);

    AbstractGroupKeyMessage request =
        AbstractGroupKeyMessage.deserialize(
            streamMessage.getSerializedContent(), streamMessage.getMessageType());
    Address sender = streamMessage.getPublisherId();
    Address recipient =
        KeyExchangeUtil.getRecipientFromKeyExchangeStreamId(streamMessage.getStreamId());

    // Check that the recipient of the request is a valid publisher of the stream
    if (!addressValidityUtil.isValidPublisher(request.getStreamId(), recipient)) {
      throw ValidationException.Factory.create(
          recipient + " is not a publisher on stream " + request.getStreamId(),
          ValidationException.Reason.PERMISSION_VIOLATION, streamMessage
      );
    }

    // Check that the sender of the request is a valid subscriber of the stream
    if (!addressValidityUtil.isValidSubscriber(request.getStreamId(), sender)) {
      throw ValidationException.Factory.create(
          sender + " is not a subscriber on stream " + request.getStreamId(),
          ValidationException.Reason.PERMISSION_VIOLATION, streamMessage
      );
    }
  }

  private void validateGroupKeyResponse(StreamMessage streamMessage) {
    if (streamMessage.getSignature() == null) {
      throw ValidationException.Factory.create(
          "Received unsigned group key response (it must be signed to avoid MitM attacks)",
          ValidationException.Reason.UNSIGNED_NOT_ALLOWED, streamMessage
      );
    }

    assertKeyExchangeStream(streamMessage);

    // Signatures on key exchange messages are checked regardless of policy setting
    assertValidSignature(streamMessage);

    AbstractGroupKeyMessage response =
        AbstractGroupKeyMessage.deserialize(
            streamMessage.getSerializedContent(), streamMessage.getMessageType());
    Address sender = streamMessage.getPublisherId();
    Address recipient =
        KeyExchangeUtil.getRecipientFromKeyExchangeStreamId(streamMessage.getStreamId());

    // Check that the sender of the request is a valid publisher of the stream
    if (!addressValidityUtil.isValidPublisher(response.getStreamId(), sender)) {
      throw ValidationException.Factory.create(
          sender + " is not a publisher on stream " + response.getStreamId(),
          ValidationException.Reason.PERMISSION_VIOLATION, streamMessage
      );
    }

    // Check that the recipient of the request is a valid subscriber of the stream
    if (!addressValidityUtil.isValidSubscriber(response.getStreamId(), recipient)) {
      throw ValidationException.Factory.create(
          recipient + " is not a subscriber on stream " + response.getStreamId(),
          ValidationException.Reason.PERMISSION_VIOLATION, streamMessage
      );
    }
  }

  private void validateGroupKeyAnnounce(StreamMessage streamMessage) {
    // Announce messages can appear in key exchange streams and normal streams and are validated
    // differently
    if (KeyExchangeUtil.isKeyExchangeStreamId(streamMessage.getStreamId())) {
      // Validate using the same logic as GroupKeyResponse
      validateGroupKeyResponse(streamMessage);
    } else {
      // Validate like a StreamMessage (except always reject unsigned)
      if (streamMessage.getSignature() == null) {
        throw ValidationException.Factory.create(
            "Received unsigned group key response (it must be signed to avoid MitM attacks)",
            ValidationException.Reason.UNSIGNED_NOT_ALLOWED, streamMessage
        );
      }
      validateStreamMessage(streamMessage);
    }
  }

  private static void assertKeyExchangeStream(StreamMessage streamMessage) {
    if (!KeyExchangeUtil.isKeyExchangeStreamId(streamMessage.getStreamId())) {
      throw ValidationException.Factory.create(
          "Group key requests can only occur on stream ids of form "
              + KeyExchangeUtil.KEY_EXCHANGE_STREAM_PREFIX
              + "{address}",
          ValidationException.Reason.INVALID_MESSAGE, streamMessage
      );
    }
  }

  private Stream getStream(String streamId) {
    Stream s = safeGetStreamCache().get(streamId);
    if (s == null) {
      s = getStreamFunction.apply(streamId);
      safeGetStreamCache().put(streamId, s);
    }
    return s;
  }

  public void clearAndClose() {
    safeGetStreamCache().clearAndClose();
    addressValidityUtil.clearAndClose();
  }
}
