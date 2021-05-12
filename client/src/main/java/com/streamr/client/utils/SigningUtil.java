package com.streamr.client.utils;

import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.ethereum.common.Address;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public final class SigningUtil {
  private SigningUtil() {}

  public static StreamMessage signStreamMessage(
      final BigInteger privateKey, final StreamMessage msg) {
    final String signature =
        sign(privateKey, getPayloadToSignOrVerify(msg, StreamMessage.SignatureType.ETH));
    return new StreamMessage.Builder(msg)
        .withSignature(signature)
        .withSignatureType(StreamMessage.SignatureType.ETH)
        .createStreamMessage();
  }

  public static String sign(final BigInteger privateKey, final String data) {
    final ECKeyPair account = ECKeyPair.create(privateKey);
    byte[] msg = data.getBytes();
    Sign.SignatureData sign = Sign.signPrefixedMessage(msg, account);
    byte[] result = new byte[65];
    System.arraycopy(sign.getR(), 0, result, 0, 32);
    System.arraycopy(sign.getS(), 0, result, 32, 32);
    System.arraycopy(sign.getV(), 0, result, 64, 1);
    return Numeric.toHexString(result);
  }

  public static boolean hasValidSignature(StreamMessage msg) {
    if (msg.getSignature() == null) {
      return false;
    }
    String payload = getPayloadToSignOrVerify(msg, msg.getSignatureType());
    try {
      return verify(payload, msg.getSignature(), msg.getPublisherId());
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

  private static boolean verify(String data, String signature, Address address)
      throws SignatureException {
    byte[] messageHash = Sign.getEthereumMessageHash(data.getBytes());
    Address recovered = recoverAddress(messageHash, signature);
    return recovered.equals(address);
  }

  private static Address recoverAddress(byte[] messageHash, String signatureHex)
      throws SignatureException {
    byte[] source = Numeric.hexStringToByteArray(signatureHex);
    byte v = source[64];
    if (v < 27) {
      v += 27;
    }
    byte[] r = Arrays.copyOfRange(source, 0, 32);
    byte[] s = Arrays.copyOfRange(source, 32, 64);
    Sign.SignatureData signature = new Sign.SignatureData(v, r, s);
    for (byte i = 0; i < 4; i++) {
      BigInteger publicKey;
      try {
        publicKey = Sign.signedMessageHashToKey(messageHash, signature);
      } catch (SignatureException e) {
        continue;
      }
      if (publicKey != null) {
        String hex = Keys.getAddress(publicKey);
        return new Address(hex);
      }
    }
    throw new SignatureException("Address recovery from signature failed.");
  }
}
