package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.exceptions.InvalidGroupKeyException;
import com.streamr.client.protocol.exceptions.UnableToDecryptException;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.testing.TestingContentX;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

class EncryptionUtilTest {
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
  private final String serializedPlaintextContent = "{\"foo\":\"bar\"}";
  private final byte[] plaintextBytes = "some random text".getBytes(StandardCharsets.UTF_8);

  private StreamMessage streamMessage;
  private EncryptionUtil util;
  private GroupKey key;

  @BeforeEach
  void setup() {
    MessageId messageId =
        new MessageId.Builder()
            .withStreamId("stream-id")
            .withStreamPartition(0)
            .withTimestamp(1L)
            .withSequenceNumber(0L)
            .withPublisherId(PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    streamMessage =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(new MessageRef(0L, 0L))
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .createStreamMessage();
    util = new EncryptionUtil();
    key = GroupKey.generate();
  }

  @Test
  void rsaDecryptionAfterEncryptionEqualsTheInitialPlaintext() throws UnableToDecryptException {
    String ciphertext =
        EncryptionUtil.encryptWithPublicKey(plaintextBytes, util.getPublicKeyAsPemString());
    assertArrayEquals(plaintextBytes, util.decryptWithPrivateKey(ciphertext));
  }

  @Test
  void rsaDecryptionAfterEncryptionEqualsTheInitialPlaintextHexString()
      throws UnableToDecryptException {

    String ciphertext =
        EncryptionUtil.encryptWithPublicKey(
            Numeric.toHexStringNoPrefix(plaintextBytes), util.getPublicKeyAsPemString());
    assertArrayEquals(plaintextBytes, util.decryptWithPrivateKey(ciphertext));
  }

  @Test
  void rsaDecryptionAfterEncryptionEqualsTheInitialPlaintextStreamMessage()
      throws UnableToDecryptException {

    streamMessage =
        EncryptionUtil.encryptWithPublicKey(streamMessage, util.getPublicKeyAsPemString());

    assertNotEquals(serializedPlaintextContent, streamMessage.getSerializedContent());
    assertEquals(StreamMessage.EncryptionType.RSA, streamMessage.getEncryptionType());
    streamMessage = util.decryptWithPrivateKey(streamMessage);
    assertEquals(serializedPlaintextContent, streamMessage.getSerializedContent());
    assertEquals(TestingContentX.mapWithValue("foo", "bar"), streamMessage.getParsedContent());
    assertEquals(StreamMessage.EncryptionType.NONE, streamMessage.getEncryptionType());
  }

  @Test
  void rsaDecryptionAfterEncryptionEqualsTheInitialPlaintextGroupKey()
      throws InvalidGroupKeyException, UnableToDecryptException {
    EncryptedGroupKey encryptedKey = EncryptionUtil.encryptWithPublicKey(key, util.getPublicKey());
    assertEquals(key.getGroupKeyId(), encryptedKey.getGroupKeyId());
    assertNotEquals(key.getGroupKeyHex(), encryptedKey.getEncryptedGroupKeyHex());

    GroupKey original = util.decryptWithPrivateKey(encryptedKey);
    assertEquals(key, original);
  }

  @Test
  void aesEncryptionPreservesSizePlusIV() {

    byte[] ciphertext =
        DatatypeConverter.parseHexBinary(EncryptionUtil.encrypt(plaintextBytes, key));
    assertEquals(plaintextBytes.length + 16, ciphertext.length);
  }

  @Test
  void multipleSameEncryptCallsUseDifferentsIVsAndProduceDifferentCiphertexts() {
    String ciphertext1 = EncryptionUtil.encrypt(plaintextBytes, key);
    String ciphertext2 = EncryptionUtil.encrypt(plaintextBytes, key);

    assertNotEquals(ciphertext2.substring(0, 32), ciphertext1.substring(0, 32));
    assertNotEquals(ciphertext2.substring(32), ciphertext1.substring(32));
  }

  @Test
  void aesDecryptionAfterEncryptionEqualsTheInitialPlaintext() throws Exception {
    String ciphertext = EncryptionUtil.encrypt(plaintextBytes, key);
    assertArrayEquals(plaintextBytes, EncryptionUtil.decrypt(ciphertext, key));
  }

  @Test
  void encryptStreamMessageEncryptsTheMessage() throws InvalidGroupKeyException {
    streamMessage = EncryptionUtil.encryptStreamMessage(streamMessage, key);
    assertNotEquals(serializedPlaintextContent, streamMessage.getSerializedContent());
    assertEquals(StreamMessage.EncryptionType.AES, streamMessage.getEncryptionType());
  }

  @Test
  void encryptStreamMessageThenDecryptStreamMessageEqualsOriginalMessage()
      throws UnableToDecryptException, InvalidGroupKeyException {

    streamMessage = EncryptionUtil.encryptStreamMessage(streamMessage, key);
    streamMessage = EncryptionUtil.decryptStreamMessage(streamMessage, key);
    assertEquals(serializedPlaintextContent, streamMessage.getSerializedContent());
    assertEquals(TestingContentX.mapWithValue("foo", "bar"), streamMessage.getParsedContent());
    assertEquals(StreamMessage.EncryptionType.NONE, streamMessage.getEncryptionType());
  }

  @Test
  void encryptGroupKeyEncryptsTheGroupKey() throws Exception {
    GroupKey keyToEncrypt = GroupKey.generate();
    GroupKey keyToEncryptWith = key;

    EncryptedGroupKey encryptedKey = EncryptionUtil.encryptGroupKey(keyToEncrypt, keyToEncryptWith);
    assertEquals(keyToEncrypt.getGroupKeyId(), encryptedKey.getGroupKeyId());
    assertNotEquals(keyToEncrypt.getGroupKeyHex(), encryptedKey.getEncryptedGroupKeyHex());
    assertNotEquals(keyToEncryptWith.getGroupKeyHex(), encryptedKey.getEncryptedGroupKeyHex());

    GroupKey original = EncryptionUtil.decryptGroupKey(encryptedKey, keyToEncryptWith);
    assertEquals(keyToEncrypt, original);
  }

  @Test
  void doesNotThrowWhenValidKeysPassedToConstructor() {
    KeyPair keyPair = EncryptionUtil.generateKeyPair();
    EncryptionUtil eu =
        new EncryptionUtil(
            (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
    assertNotNull(eu);
  }

  @Test
  void doesNotThrowWhenBothParamsAreNullOnAutoKeyGeneration() {
    assertNotNull(new EncryptionUtil());
  }

  @Test
  void validatePublicKeyThrowsOnInvalidKey() {
    RuntimeException e =
        assertThrows(
            RuntimeException.class,
            () -> {
              EncryptionUtil.validatePublicKey("wrong public key");
            });
    assertEquals("Must be a valid RSA public key in the PEM format.", e.getMessage());
  }

  @Test
  void validatePublicKeyDoesNotThrowOnValidKey() {
    RSAKeyPair pair = RSAKeyPair.create();
    EncryptionUtil.validatePublicKey(pair.publicKeyToPem());
  }
}
