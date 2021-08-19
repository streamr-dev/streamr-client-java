package com.streamr.client.protocol.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.testing.TestingContentX;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

class SigningUtilTest {
  private BigInteger privateKey;
  private MessageId msgId;
  private static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");

  @BeforeEach
  void setup() {
    privateKey =
        new BigInteger("23bead9b499af21c4c16e4511b3b6b08c3e22e76e0591f5ab5ba8d4c3a5b1820", 16);
    final ECKeyPair account = ECKeyPair.create(privateKey);
    final String addr = Keys.getAddress(account.getPublicKey());
    final String hex = Numeric.prependHexPrefix(addr);
    assert new Address(hex)
        .toString()
        .equals("0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5".toLowerCase());

    msgId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withStreamPartition(0)
            .withTimestamp(425235315L)
            .withSequenceNumber(0L)
            .withPublisherId(PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
  }

  @Test
  void shouldCorrectlySignArbitraryData() {
    String payload = "data-to-sign";
    String signature = SigningUtil.sign(privateKey, payload);

    assertEquals(
        "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b",
        signature);
  }

  @Test
  void shouldCorrectlySignStreamMessageWithNullPreviousRef() {
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .createStreamMessage();
    String expectedPayload = "streamId04252353150" + PUBLISHER_ID + "msgChainId{\"foo\":\"bar\"}";

    msg = SigningUtil.signStreamMessage(privateKey, msg);

    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals(SigningUtil.sign(privateKey, expectedPayload), msg.getSignature());
  }

  @Test
  void shouldCorrectlySignStreamMessageWithNonNullPreviousRef() {
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(new MessageRef(100, 1))
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .createStreamMessage();
    String expectedPayload =
        "streamId04252353150" + PUBLISHER_ID + "msgChainId1001{\"foo\":\"bar\"}";

    msg = SigningUtil.signStreamMessage(privateKey, msg);

    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals(SigningUtil.sign(privateKey, expectedPayload), msg.getSignature());
  }

  @Test
  void shouldCorrectlySignStreamMessageWithNewGroupKey() {
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(new MessageRef(100, 1))
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .withNewGroupKey(new EncryptedGroupKey("groupKeyId", "keyHex"))
            .createStreamMessage();
    String expectedPayload =
        "streamId04252353150"
            + PUBLISHER_ID
            + "msgChainId1001{\"foo\":\"bar\"}[\"groupKeyId\",\"keyHex\"]";

    msg = SigningUtil.signStreamMessage(privateKey, msg);

    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals(SigningUtil.sign(privateKey, expectedPayload), msg.getSignature());
  }

  @Test
  void returnsFalseIfNoSignature() {
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .createStreamMessage();
    assertTrue(!SigningUtil.hasValidSignature(msg));
  }

  @Test
  void returnsFalseIfWrongSignature() {
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .withSignature(
                "0x787cd72924153c88350e808de68b68c88030cbc34d053a5c696a5893d5e6fec1687c1b6205ec99aeb3375a81bf5cb8857ae39c1b55a41b32ed6399ae8da456a61b")
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    assertTrue(!SigningUtil.hasValidSignature(msg));
  }

  @Test
  void returnsTrueIfCorrectSignature() {
    MessageId msgId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withStreamPartition(0)
            .withTimestamp(425235315L)
            .withSequenceNumber(0L)
            .withPublisherId(new Address("0xa5374e3C19f15E1847881979Dd0C6C9ffe846BD5"))
            .withMsgChainId("msgChainId")
            .createMessageId();
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(null)
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("foo", "bar")))
            .createStreamMessage();
    msg = SigningUtil.signStreamMessage(privateKey, msg);

    assertTrue(SigningUtil.hasValidSignature(msg));
  }

  @Test
  void returnsTrueForCorrectSignatureOfPublisherAddressHasUpperAndLowerCaseLetters() {
    Address address1 = new Address("0x752C8dCAC0788759aCB1B4BB7A9103596BEe3e6c");
    MessageId msgId =
        new MessageId.Builder()
            .withStreamId("ogzCJrTdQGuKQO7nkLd3Rw")
            .withStreamPartition(0)
            .withTimestamp(1567003338767L)
            .withSequenceNumber(2L)
            .withPublisherId(address1)
            .withMsgChainId("kxYyLiSUQO0SRvMx6gA1")
            .createMessageId();
    StreamMessage msg =
        new StreamMessage.Builder()
            .withMessageId(msgId)
            .withPreviousMessageRef(new MessageRef(1567003338767L, 1L))
            .withContent(TestingContentX.fromJsonMap(TestingContentX.mapWithValue("numero", 86)))
            .withSignature(
                "0xc97f1fbb4f506a53ecb838db59017f687892494a9073315f8a187846865bf8325333315b116f1142921a97e49e3881eced2b176c69f9d60666b98b7641ad11e01b")
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .createStreamMessage();

    assertTrue(SigningUtil.hasValidSignature(msg));
  }
}
