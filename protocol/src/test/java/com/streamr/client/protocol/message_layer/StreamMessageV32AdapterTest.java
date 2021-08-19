package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.utils.Address;
import com.streamr.client.protocol.utils.EncryptedGroupKey;
import com.streamr.client.testing.TestingContentX;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMessageV32AdapterTest {
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
  private static final int VERSION = 32;
  private StreamMessage msg;

  @BeforeEach
  void setup() {
    // Message with minimal fields
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("streamId")
            .withStreamPartition(0)
            .withTimestamp(123L)
            .withSequenceNumber(0)
            .withPublisherId(PUBLISHER_ID)
            .withMsgChainId("msgChainId")
            .createMessageId();
    msg =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(null)
            .withContent(TestingContentX.emptyMessage())
            .createStreamMessage();
  }

  @Test
  void serializeMinimalMessage() {
    String expectedJson =
        "[32,[\"streamId\",0,123,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"msgChainId\"],null,27,0,0,null,\"{}\",null,0,null]";

    assertEquals(expectedJson, StreamMessageAdapter.serialize(msg, VERSION));
    assertEquals(
        msg, StreamMessageAdapter.deserialize(StreamMessageAdapter.serialize(msg, VERSION)));
  }

  @Test
  void serializeMaximalMessage() {
    String expectedJson =
        "[32,[\"streamId\",0,123,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"msgChainId\"],[122,0],27,0,2,\"groupKeyId\",\"encrypted-content\",\"[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex-cached\\\"]\",2,\"signature\"]";
    msg =
        new StreamMessage.Builder(msg)
            .withSignature("signature")
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .withContent(TestingContentX.fromJsonString("encrypted-content"))
            .withPreviousMessageRef(new MessageRef(122L, 0))
            .withEncryptionType(StreamMessage.EncryptionType.AES)
            .withGroupKeyId("groupKeyId")
            .withNewGroupKey(
                new EncryptedGroupKey(
                    "newGroupKeyId",
                    "encryptedGroupKeyHex",
                    "[\"newGroupKeyId\",\"encryptedGroupKeyHex-cached\"]"))
            .createStreamMessage();

    assertEquals(expectedJson, StreamMessageAdapter.serialize(msg, VERSION));
    assertEquals(
        msg, StreamMessageAdapter.deserialize(StreamMessageAdapter.serialize(msg, VERSION)));
  }

  @Test
  void deserializeMinimalMessage() {
    String json =
        "[32,[\"streamId\",0,123,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"msgChainId\"],null,27,0,0,null,\"{}\",null,0,null]";
    msg = StreamMessageAdapter.deserialize(json);
    assertEquals("streamId", msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(123L, msg.getTimestamp());
    assertEquals(new Date(123L), msg.getTimestampAsDate());
    assertEquals(0, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals("msgChainId", msg.getMsgChainId());
    assertEquals(null, msg.getPreviousMessageRef());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertTrue(msg.getParsedContent() instanceof Map);
    assertEquals(StreamMessage.SignatureType.NONE, msg.getSignatureType());
    assertEquals(null, msg.getSignature());
    assertEquals(json, StreamMessageAdapter.serialize(msg, VERSION));
  }

  @Test
  void deserializeMaximalMessage() {
    String json =
        "[32,[\"streamId\",0,123,0,\""
            + PUBLISHER_ID
            + "\",\"msgChainId\"],[122,0],27,0,2,\"groupKeyId\",\"encrypted-content\",\"[\\\"newGroupKeyId\\\",\\\"encryptedGroupKeyHex\\\"]\",2,\"signature\"]";
    msg = StreamMessageAdapter.deserialize(json);
    assertEquals("streamId", msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(123L, msg.getTimestamp());
    assertEquals(new Date(123L), msg.getTimestampAsDate());
    assertEquals(0, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals("msgChainId", msg.getMsgChainId());
    assertEquals(new MessageRef(122L, 0), msg.getPreviousMessageRef());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.AES, msg.getEncryptionType());
    assertEquals("encrypted-content", msg.getSerializedContent());
    assertEquals(
        new EncryptedGroupKey("newGroupKeyId", "encryptedGroupKeyHex"), msg.getNewGroupKey());
    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals("signature", msg.getSignature());
    assertEquals(json, StreamMessageAdapter.serialize(msg, VERSION));
  }
}
