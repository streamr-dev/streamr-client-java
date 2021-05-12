package com.streamr.client.utils;

import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.ethereum.crypto.Sign;
import java.math.BigInteger;
import java.security.SignatureException;

public final class SigningUtil {
  private SigningUtil() {}

  public static StreamMessage signStreamMessage(
      final BigInteger privateKey, final StreamMessage msg) {
    final String signature =
        Sign.sign(privateKey, getPayloadToSignOrVerify(msg, StreamMessage.SignatureType.ETH));
    return new StreamMessage.Builder(msg)
        .withSignature(signature)
        .withSignatureType(StreamMessage.SignatureType.ETH)
        .createStreamMessage();
  }

  public static boolean hasValidSignature(StreamMessage msg) {
    if (msg.getSignature() == null) {
      return false;
    }
    String payload = getPayloadToSignOrVerify(msg, msg.getSignatureType());
    try {
      return Sign.verify(payload, msg.getSignature(), msg.getPublisherId());
    } catch (SignatureException e) {
      throw new SignatureFailedException(e.getMessage());
    }
  }

  private static String getPayloadToSignOrVerify(
      StreamMessage msg, StreamMessage.SignatureType signatureType) {
    if (signatureType == StreamMessage.SignatureType.ETH_LEGACY) {
      StringBuilder sb = new StringBuilder(msg.getStreamId());
      sb.append(msg.getStreamPartition());
      sb.append(msg.getTimestamp());
      sb.append(msg.getPublisherId());
      sb.append(msg.getSerializedContent());
      return sb.toString();
    } else if (signatureType == StreamMessage.SignatureType.ETH) {
      StringBuilder sb = new StringBuilder(msg.getStreamId());
      sb.append(msg.getStreamPartition());
      sb.append(msg.getTimestamp());
      sb.append(msg.getSequenceNumber());
      sb.append(msg.getPublisherId());
      sb.append(msg.getMsgChainId());
      if (msg.getPreviousMessageRef() != null) {
        sb.append(msg.getPreviousMessageRef().getTimestamp());
        sb.append(msg.getPreviousMessageRef().getSequenceNumber());
      }
      sb.append(msg.getSerializedContent());
      if (msg.getNewGroupKey() != null) {
        sb.append(msg.getNewGroupKey().serialize());
      }
      return sb.toString();
    }
    throw new UnsupportedSignatureTypeException(signatureType);
  }
}
