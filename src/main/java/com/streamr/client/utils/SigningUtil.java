package com.streamr.client.utils;

import com.streamr.client.exceptions.SignatureFailedException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.exceptions.UnsupportedSignatureTypeException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Set;

public class SigningUtil {
    private static final String SIGN_MAGIC = "\u0019Ethereum Signed Message:\n";
    private ECKey account;

    public SigningUtil(ECKey account) {
        this.account = account;
    }

    public void signStreamMessage(StreamMessage msg, StreamMessage.SignatureType signatureType) {
        if (msg.getVersion() != 30) {
            throw new UnsupportedMessageException("Can only sign most recent StreamMessage version (30).");
        }
        String signature = sign(getPayloadToSignOrVerify(msg, signatureType), account);
        msg.setSignatureType(signatureType);
        msg.setSignature(signature);
    }

    public void signStreamMessage(StreamMessage msg) {
        signStreamMessage(msg, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH);
    }

    public static String sign(String data, ECKey account){
        ECKey.ECDSASignature sig = account.sign(calculateMessageHash(data));
        return "0x" + Hex.encodeHexString(ByteUtil.merge(
                ByteUtil.bigIntegerToBytes(sig.r, 32),
                ByteUtil.bigIntegerToBytes(sig.s, 32),
                new byte[]{sig.v}));
    }

    public static boolean hasValidSignature(StreamMessage msg, Set<String> publishers) {
        if (msg.getSignature() == null) {
            return false;
        }
        String payload = getPayloadToSignOrVerify(msg, msg.getSignatureType());
        try {
            boolean valid = verify(payload, msg.getSignature(), msg.getPublisherId());
            return valid && publishers.contains(msg.getPublisherId());
        } catch (SignatureException | DecoderException e) {
            throw new SignatureFailedException(e.getMessage());
        }
    }

    private static String getPayloadToSignOrVerify(StreamMessage msg, StreamMessage.SignatureType signatureType) {
        if (signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH_LEGACY) {
            StringBuilder sb = new StringBuilder(msg.getStreamId());
            sb.append(msg.getStreamPartition());
            sb.append(msg.getTimestamp());
            sb.append(msg.getPublisherId());
            sb.append(msg.getSerializedContent());
            return sb.toString();
        } else if (signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH) {
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
            return sb.toString();
        }
        throw new UnsupportedSignatureTypeException(signatureType);
    }

    private static byte[] calculateMessageHash(String message){
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        String prefix = SIGN_MAGIC + messageBytes.length;
        byte[] toHash = ByteUtil.merge(prefix.getBytes(), messageBytes);
        return HashUtil.sha3(toHash);
    }

    private static boolean verify(String data, String signature, String address) throws SignatureException, DecoderException {
        return recoverAddress(calculateMessageHash(data), signature).toLowerCase().equals(address.toLowerCase());
    }

    private static String recoverAddress(byte[] messageHash, String signatureHex) throws SignatureException, DecoderException {
        byte[] signature = Hex.decodeHex(signatureHex.replace("0x", "").toCharArray());

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        byte v = signature[64];
        System.arraycopy(signature, 0, r, 0, r.length);
        System.arraycopy(signature, 32, s, 0, s.length);

        ECKey.ECDSASignature signatureObj = ECKey.ECDSASignature.fromComponents(r, s, v);
        return "0x" + Hex.encodeHexString(ECKey.signatureToKey(messageHash, signatureObj.toBase64()).getAddress());
    }
}
