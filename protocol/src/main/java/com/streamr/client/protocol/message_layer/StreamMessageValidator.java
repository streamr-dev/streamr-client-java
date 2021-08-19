package com.streamr.client.protocol.message_layer;

import com.streamr.client.protocol.options.SigningOptions.SignatureVerificationPolicy;
import com.streamr.client.protocol.rest.Stream;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.AddressValidityUtil;
import com.streamr.client.protocol.utils.KeyExchangeUtil;
import com.streamr.client.protocol.utils.SigningUtil;
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
        throw new ValidationException(msg, ValidationException.Reason.INVALID_MESSAGE);
    }
  }

  private void validateStreamMessage(StreamMessage msg) {
    Stream stream = getStream(msg.getStreamId());

    // Checks against stream metadata
    if (stream.requiresSignedData() && msg.getSignature() == null) {
      throw new ValidationException(
          msg,
          ValidationException.Reason.POLICY_VIOLATION,
          "Stream " + stream.getId() + " requires messages to be signed.");
    }
    if (stream.requiresEncryptedData()
        && msg.getEncryptionType() == StreamMessage.EncryptionType.NONE) {
      throw new ValidationException(
          msg,
          ValidationException.Reason.POLICY_VIOLATION,
          "Stream " + stream.getId() + " requires messages to be encrypted.");
    }
    if (msg.getStreamPartition() < 0 || msg.getStreamPartition() >= stream.getPartitions()) {
      throw new ValidationException(
          msg,
          ValidationException.Reason.INVALID_MESSAGE,
          "Partition "
              + msg.getStreamPartition()
              + " is out of range (0.."
              + (stream.getPartitions() - 1)
              + ")");
    }

    assertValidSignatureAccordingToPolicy(msg);

    // Check publisher. Note that this can only be checked on signed messages.
    if (msg.getSignature() != null) {
      Address sender = msg.getPublisherId();

      // Check that the sender of the message is a valid publisher of the stream
      if (!addressValidityUtil.isValidPublisher(msg.getStreamId(), sender)) {
        throw new ValidationException(
            msg,
            ValidationException.Reason.PERMISSION_VIOLATION,
            sender + " is not a publisher on stream " + msg.getStreamId());
      }
    }
  }

  private void assertValidSignature(StreamMessage msg) throws ValidationException {
    boolean valid;
    try {
      valid = SigningUtil.hasValidSignature(msg);
      if (!valid) {
        throw new ValidationException(msg, ValidationException.Reason.INVALID_SIGNATURE);
      }
    } catch (Exception e) {
      throw new ValidationException(
          msg, ValidationException.Reason.INVALID_SIGNATURE, e.getMessage());
    }
  }

  private void assertValidSignatureAccordingToPolicy(StreamMessage msg) throws ValidationException {
    if (signatureVerificationPolicy == SignatureVerificationPolicy.NEVER) {
      // Always pass
      return;
    }

    if (signatureVerificationPolicy == SignatureVerificationPolicy.ALWAYS
        && msg.getSignature() == null) {
      throw new ValidationException(msg, ValidationException.Reason.POLICY_VIOLATION);
    }

    if (msg.getSignature() != null) {
      assertValidSignature(msg);
    }
  }

  private void validateGroupKeyRequest(StreamMessage streamMessage) {
    if (streamMessage.getSignature() == null) {
      throw new ValidationException(streamMessage, ValidationException.Reason.UNSIGNED_NOT_ALLOWED);
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
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.PERMISSION_VIOLATION,
          recipient + " is not a publisher on stream " + request.getStreamId());
    }

    // Check that the sender of the request is a valid subscriber of the stream
    if (!addressValidityUtil.isValidSubscriber(request.getStreamId(), sender)) {
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.PERMISSION_VIOLATION,
          sender + " is not a subscriber on stream " + request.getStreamId());
    }
  }

  private void validateGroupKeyResponse(StreamMessage streamMessage) {
    if (streamMessage.getSignature() == null) {
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.UNSIGNED_NOT_ALLOWED,
          "Received unsigned group key response (it must be signed to avoid MitM attacks)");
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
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.PERMISSION_VIOLATION,
          sender + " is not a publisher on stream " + response.getStreamId());
    }

    // Check that the recipient of the request is a valid subscriber of the stream
    if (!addressValidityUtil.isValidSubscriber(response.getStreamId(), recipient)) {
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.PERMISSION_VIOLATION,
          recipient + " is not a subscriber on stream " + response.getStreamId());
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
        throw new ValidationException(
            streamMessage,
            ValidationException.Reason.UNSIGNED_NOT_ALLOWED,
            "Received unsigned group key response (it must be signed to avoid MitM attacks)");
      }
      validateStreamMessage(streamMessage);
    }
  }

  private static void assertKeyExchangeStream(StreamMessage streamMessage) {
    if (!KeyExchangeUtil.isKeyExchangeStreamId(streamMessage.getStreamId())) {
      throw new ValidationException(
          streamMessage,
          ValidationException.Reason.INVALID_MESSAGE,
          "Group key requests can only occur on stream ids of form "
              + KeyExchangeUtil.KEY_EXCHANGE_STREAM_PREFIX
              + "{address}");
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
