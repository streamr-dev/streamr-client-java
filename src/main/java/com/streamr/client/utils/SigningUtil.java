package com.streamr.client.utils;

import com.streamr.client.exceptions.UnsupportedSignatureTypeException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public final class SigningUtil {
  private static final String SIGN_MAGIC = "\u0019Ethereum Signed Message:\n";

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
    byte[] msg = data.getBytes(StandardCharsets.UTF_8);
    Sign.SignatureData sign = Sign.signPrefixedMessage(msg, account);
    SignatureData signature = new SignatureData(sign);
    return signature.toHex();
  }

  public static boolean hasValidSignature(StreamMessage msg) {
    if (msg.getSignature() == null) {
      return false;
    }
    String payload = getPayloadToSignOrVerify(msg, msg.getSignatureType());
    return verify(payload, msg.getSignature(), msg.getPublisherId());
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

  private static byte[] calculateMessageHash(String message) {
    int msgLen = message.getBytes(StandardCharsets.UTF_8).length;
    String s = String.format("%s%d%s", SIGN_MAGIC, msgLen, message);
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    return Hash.sha3(bytes);
  }

  private static boolean verify(String data, String signature, Address address) {
    byte[] messageHash = calculateMessageHash(data);
    Address result = recoverAddress(messageHash, signature, address);
    return address.equals(result);
  }

  private static Address recoverAddress(byte[] messageHash, String signatureHex, Address original) {
    final ECDSASignature signature;

    byte[] source = Numeric.hexStringToByteArray(signatureHex);
    BigInteger r = toBigInteger(source, 0, 32);
    BigInteger s = toBigInteger(source, 32, 64);
    signature = new ECDSASignature(r, s);
    for (byte i = 0; i < 4; i++) {
      BigInteger publicKey;
      try {
        publicKey = Sign.recoverFromSignature(i, signature, messageHash);
      } catch (RuntimeException e) {
        continue;
      }
      if (publicKey != null) {
        String hex = Keys.getAddress(publicKey);
        String hexWithPrefix = Numeric.prependHexPrefix(hex);
        Address address = new Address(hexWithPrefix);
        if (address.equals(original)) {
          return address;
        }
      }
    }
    return null;
  }

  private static BigInteger toBigInteger(final byte[] source, final int from, final int to) {
    byte[] bytes = Arrays.copyOfRange(source, from, to);
    BigInteger i = new BigInteger(1, bytes);
    return i;
  }
}
