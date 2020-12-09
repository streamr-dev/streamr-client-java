package com.streamr.client.utils;

import com.streamr.client.exceptions.SignatureFailedException;
import com.streamr.client.exceptions.UnsupportedSignatureTypeException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.crypto.ECKey;
import org.ethereum.util.ByteUtil;
import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

public class SigningUtil {
    private static final String SIGN_MAGIC = "\u0019Ethereum Signed Message:\n";
    private final ECKey account;

    public SigningUtil(ECKey account) {
        this.account = account;
    }

    public void signStreamMessage(StreamMessage msg, StreamMessage.SignatureType signatureType) {
        String signature = sign(getPayloadToSignOrVerify(msg, signatureType), account);
        msg.setSignatureFields(signature, signatureType);
    }

    public void signStreamMessage(StreamMessage msg) {
        signStreamMessage(msg, StreamMessage.SignatureType.ETH);
    }

    public static String sign(String data, ECKey account) {
        byte[] hash = calculateMessageHash(data);
        ECKey.ECDSASignature sig = account.sign(hash);
        byte[] r = ByteUtil.bigIntegerToBytes(sig.r, 32);
        byte[] s = ByteUtil.bigIntegerToBytes(sig.s, 32);
        byte[] v = { sig.v };
        byte[] bytes = ByteUtil.merge(r, s, v);
        return "0x" + Hex.encodeHexString(bytes);
    }

    public static boolean hasValidSignature(StreamMessage msg) {
        if (msg.getSignature() == null) {
            return false;
        }
        String payload = getPayloadToSignOrVerify(msg, msg.getSignatureType());
        try {
            return verify(payload, msg.getSignature(), msg.getPublisherId());
        } catch (SignatureException | DecoderException e) {
            throw new SignatureFailedException(e.getMessage());
        }
    }

    private static String getPayloadToSignOrVerify(StreamMessage msg, StreamMessage.SignatureType signatureType) {
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

    private static byte[] calculateMessageHash(String message){
        int msgLen = message.getBytes(StandardCharsets.UTF_8).length;
        String s = String.format("%s%d%s", SIGN_MAGIC, msgLen, message);
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return Hash.sha3(bytes);
    }

    private static boolean verify(String data, String signature, Address address) throws SignatureException, DecoderException {
        return recoverAddress(calculateMessageHash(data), signature).equals(address);
    }

    private static Address recoverAddress(byte[] messageHash, String signatureHex) throws SignatureException, DecoderException {
        byte[] signature = Hex.decodeHex(signatureHex.replace("0x", "").toCharArray());

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        byte v = signature[64];
        System.arraycopy(signature, 0, r, 0, r.length);
        System.arraycopy(signature, 32, s, 0, s.length);

        ECKey.ECDSASignature signatureObj = ECKey.ECDSASignature.fromComponents(r, s, v);
        return new Address(ECKey.signatureToKey(messageHash, signatureObj.toBase64()).getAddress());
    }
}
