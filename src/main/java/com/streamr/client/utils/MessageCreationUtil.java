package com.streamr.client.utils;

import com.streamr.client.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.message_layer.*;
import com.streamr.client.protocol.message_layer.StreamMessage.EncryptionType;
import com.streamr.client.rest.Stream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageCreationUtil {
    private static final Logger log = LogManager.getLogger();
    private static final SecureRandom SRAND = new SecureRandom();

    private final String publisherId;
    private final String msgChainId;
    private final SigningUtil signingUtil;
    private SecretKey groupKey;

    private final HashMap<String, MessageRef> refsPerStreamAndPartition = new HashMap<>();

    private final HashMap<String, Integer> cachedHashes = new HashMap<>();

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil) {
        this(publisherId, signingUtil, null);
    }

    public MessageCreationUtil(String publisherId, SigningUtil signingUtil, String groupKeyHex) {
        this.publisherId = publisherId;
        msgChainId = RandomStringUtils.randomAlphanumeric(20);
        this.signingUtil = signingUtil;
        if (groupKeyHex != null) {
            validateGroupKey(groupKeyHex);
            groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
        }
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey) {
        return createStreamMessage(stream, payload, timestamp, partitionKey, null);
    }

    public StreamMessage createStreamMessage(Stream stream, Map<String, Object> payload, Date timestamp, String partitionKey, String groupKeyHex) {
        if (groupKeyHex != null) {
            validateGroupKey(groupKeyHex);
        }

        EncryptionType encryptionType;
        String content;
        if (groupKey != null && groupKeyHex != null) {
            encryptionType = EncryptionType.NEW_KEY_AND_AES;
            byte[] groupKeyBytes = DatatypeConverter.parseHexBinary(groupKeyHex);
            byte[] payloadBytes = HttpUtils.mapAdapter.toJson(payload).getBytes(StandardCharsets.UTF_8);
            byte[] plaintext = new byte[groupKeyBytes.length + payloadBytes.length];
            System.arraycopy(groupKeyBytes, 0, plaintext, 0, groupKeyBytes.length);
            System.arraycopy(payloadBytes, 0, plaintext, groupKeyBytes.length, payloadBytes.length);
            content = encrypt(plaintext);
            groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
        } else if (groupKeyHex != null || groupKey != null) {
            if (groupKeyHex != null) {
                groupKey = new SecretKeySpec(DatatypeConverter.parseHexBinary(groupKeyHex), "AES");
            }
            encryptionType = EncryptionType.AES;
            content = encrypt(HttpUtils.mapAdapter.toJson(payload).getBytes(StandardCharsets.UTF_8));
        } else {
            encryptionType = EncryptionType.NONE;
            content = HttpUtils.mapAdapter.toJson(payload);
        }
        int streamPartition = getStreamPartition(stream.getPartitions(), partitionKey);
        String key = stream.getId() + streamPartition;

        long sequenceNumber = getNextSequenceNumber(key, timestamp.getTime());
        MessageID msgId = new MessageID(stream.getId(), streamPartition, timestamp.getTime(), sequenceNumber, publisherId, msgChainId);
        MessageRef prevRef = refsPerStreamAndPartition.get(key);
        StreamMessage streamMessage = new StreamMessageV31(msgId, prevRef, StreamMessage.ContentType.CONTENT_TYPE_JSON,
                encryptionType, content, StreamMessage.SignatureType.SIGNATURE_TYPE_NONE, null
        );

        refsPerStreamAndPartition.put(key, new MessageRef(timestamp.getTime(), sequenceNumber));
        if (signingUtil != null) {
            signingUtil.signStreamMessage(streamMessage);
        }
        return streamMessage;
    }

    private int hash(String partitionKey) {
        Integer hash = cachedHashes.get(partitionKey);
        if (hash == null) {
            byte[] bytes;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                bytes = md.digest(partitionKey.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            hash = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            cachedHashes.put(partitionKey, hash);
        }
        return hash;
    }

    private int getStreamPartition(int nbPartitions, String partitionKey) {
        if (nbPartitions == 0) {
            throw new Error("partitionCount is falsey!");
        } else if (nbPartitions == 1) {
            return 0;
        } else if (partitionKey != null) {
            int h = hash(partitionKey);
            return Math.abs(h) % nbPartitions;
        } else {
            return (int) Math.floor(Math.random() * nbPartitions);
        }
    }

    private long getNextSequenceNumber(String key, long timestamp) {
        MessageRef prev = refsPerStreamAndPartition.get(key);
        if (prev == null || prev.getTimestamp() != timestamp) {
            return 0L;
        }
        return prev.getSequenceNumber() + 1L;
    }

    private String encrypt(byte[] plaintext) {
        try {
            Cipher encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            byte[] iv = new byte[16];
            SRAND.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            encryptCipher.init(Cipher.ENCRYPT_MODE, groupKey, ivspec);
            byte[] ciphertext = encryptCipher.doFinal(plaintext);
            return Hex.encodeHexString(iv) + Hex.encodeHexString(ciphertext);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private static void validateGroupKey(String groupKeyHex) {
        String without0x = groupKeyHex.startsWith("0x") ? groupKeyHex.substring(2) : groupKeyHex;
        if (without0x.length() != 64) { // the key must be 256 bits long
            throw new InvalidGroupKeyException(without0x.length() * 4);
        }
    }
}
