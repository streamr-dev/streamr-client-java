package com.streamr.client.utils;

import com.streamr.client.exceptions.SignatureFailedException;
import com.streamr.client.exceptions.UnsupportedMessageException;
import com.streamr.client.exceptions.UnsupportedSignatureTypeException;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.protocol.message_layer.StreamMessageV30;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;

import java.io.IOException;

public class SigningUtil {
    private static final String SIGN_MAGIC = "\u0019Ethereum Signed Message:\n";
    private ECKey account;

    public SigningUtil(ECKey account) {
        this.account = account;
    }

    public StreamMessage getSignedStreamMessage(StreamMessage msg, StreamMessage.SignatureType signatureType) {
        if (msg.getVersion() != 30) {
            throw new UnsupportedMessageException("Can only sign most recent StreamMessage version (30).");
        }
        StreamMessageV30 msgv30 = (StreamMessageV30) msg;
        try {
            String signature = sign(getPayloadToSign(msg, signatureType), account);
            return new StreamMessageV30(msgv30.getMessageID(), msgv30.getPreviousMessageRef(), msgv30.getContentType(),
                    msgv30.getSerializedContent(), signatureType, signature);
        } catch (DecoderException | IOException e) {
            throw new SignatureFailedException(e.getMessage());
        }
    }

    public StreamMessage getSignedStreamMessage(StreamMessage msg) {
        return getSignedStreamMessage(msg, StreamMessage.SignatureType.SIGNATURE_TYPE_ETH);
    }

    private static String getPayloadToSign(StreamMessage msg, StreamMessage.SignatureType signatureType) {
        if (signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH_LEGACY) {
            return String.format("%s%s%s%s%s", msg.getStreamId(), msg.getStreamPartition(), msg.getTimestamp(),
                    msg.getPublisherId(), msg.getSerializedContent());
        } else if (signatureType == StreamMessage.SignatureType.SIGNATURE_TYPE_ETH) {
            return String.format("%s%s%s%s%s%s%s", msg.getStreamId(), msg.getStreamPartition(), msg.getTimestamp(),
                    msg.getSequenceNumber(), msg.getPublisherId(), msg.getMsgChainId(), msg.getSerializedContent());
        }
        throw new UnsupportedSignatureTypeException(signatureType);
    }

    public static String sign(String data, ECKey account) throws DecoderException {
        org.ethereum.crypto.ECKey.ECDSASignature sig = account.sign(calculateMessageHash(data));
        return "0x" + Hex.encodeHexString(ByteUtil.merge(
                ByteUtil.bigIntegerToBytes(sig.r, 32),
                ByteUtil.bigIntegerToBytes(sig.s, 32),
                new byte[]{sig.v}));
    }

    private static byte[] calculateMessageHash(String message) throws DecoderException {
        String messageHex = "0x" + Hex.encodeHexString(message.getBytes());
        byte[] messageBytes = Hex.decodeHex(messageHex.replace("0x", "").toCharArray());
        String prefix = SIGN_MAGIC + messageBytes.length;
        byte[] toHash = ByteUtil.merge(prefix.getBytes(), messageBytes);
        return HashUtil.sha3(toHash);
    }
}
