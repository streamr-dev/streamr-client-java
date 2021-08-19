package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamr.client.protocol.common.MessageRef;
import com.streamr.client.protocol.utils.Address;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMessageV31AdapterTest {
  public static final Address PUBLISHER_ID =
      new Address("0xBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
  StreamMessageAdapter adapter;
  StreamMessage msg;

  @BeforeEach
  void setup() {
    adapter = new StreamMessageAdapter();
    String serializedContent =
        "{\"desi\":\"2\",\"dir\":\"1\",\"oper\":40,\"veh\":222,\"tst\":\"2018-06-05T19:49:33Z\",\"tsi\":1528228173,\"spd\":3.6,\"hdg\":69,\"lat\":60.192258,\"long\":24.928701,\"acc\":-0.59,\"dl\":-248,\"odo\":5134,\"drst\":0,\"oday\":\"2018-06-05\",\"jrn\":885,\"line\":30,\"start\":\"22:23\"}";
    final MessageId messageId =
        new MessageId.Builder()
            .withStreamId("7wa7APtlTq6EC5iTCBy6dw")
            .withStreamPartition(0)
            .withTimestamp(1528228173462L)
            .withSequenceNumber(0)
            .withPublisherId(PUBLISHER_ID)
            .withMsgChainId("1")
            .createMessageId();
    msg =
        new StreamMessage.Builder()
            .withMessageId(messageId)
            .withPreviousMessageRef(new MessageRef(1528228170000L, 0))
            .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
            .withContent(StreamMessage.Content.Factory.withJsonAsPayload(serializedContent))
            .withEncryptionType(StreamMessage.EncryptionType.NONE)
            .withGroupKeyId(null)
            .withNewGroupKey(null)
            .withSignatureType(StreamMessage.SignatureType.ETH)
            .withSignature("signature")
            .createStreamMessage();
  }

  @Test
  void deserialize() {
    String json =
        "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"1\"],[1528228170000,0],27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]";
    msg = StreamMessageAdapter.deserialize(json);

    assertEquals("7wa7APtlTq6EC5iTCBy6dw", msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(1528228173462L, msg.getTimestamp());
    assertEquals(new Date(1528228173462L), msg.getTimestampAsDate());
    assertEquals(0, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals("1", msg.getMsgChainId());
    assertEquals(1528228170000L, msg.getPreviousMessageRef().getTimestamp());
    assertEquals(new Date(1528228170000L), msg.getPreviousMessageRef().getTimestampAsDate());
    assertEquals(0, msg.getPreviousMessageRef().getSequenceNumber());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertTrue(msg.getParsedContent() instanceof Map);
    assertEquals("2", msg.getParsedContent().get("desi"));
    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals("signature", msg.getSignature());
  }

  @Test
  void deserializeWithPreviousMessageRefNull() {
    String json =
        "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",2,\"signature\"]";
    msg = StreamMessageAdapter.deserialize(json);

    assertEquals("7wa7APtlTq6EC5iTCBy6dw", msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(1528228173462L, msg.getTimestamp());
    assertEquals(new Date(1528228173462L), msg.getTimestampAsDate());
    assertEquals(0, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals("1", msg.getMsgChainId());
    assertEquals(null, msg.getPreviousMessageRef());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertTrue(msg.getParsedContent() instanceof Map);
    assertEquals("2", msg.getParsedContent().get("desi"));
    assertEquals(StreamMessage.SignatureType.ETH, msg.getSignatureType());
    assertEquals("signature", msg.getSignature());
  }

  @Test
  void deserializeWithNoSignature() {
    String json =
        "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"1\"],null,27,0,\"{\\\"desi\\\":\\\"2\\\",\\\"dir\\\":\\\"1\\\",\\\"oper\\\":40,\\\"veh\\\":222,\\\"tst\\\":\\\"2018-06-05T19:49:33Z\\\",\\\"tsi\\\":1528228173,\\\"spd\\\":3.6,\\\"hdg\\\":69,\\\"lat\\\":60.192258,\\\"long\\\":24.928701,\\\"acc\\\":-0.59,\\\"dl\\\":-248,\\\"odo\\\":5134,\\\"drst\\\":0,\\\"oday\\\":\\\"2018-06-05\\\",\\\"jrn\\\":885,\\\"line\\\":30,\\\"start\\\":\\\"22:23\\\"}\",0,null]";
    msg = StreamMessageAdapter.deserialize(json);

    assertEquals("7wa7APtlTq6EC5iTCBy6dw", msg.getStreamId());
    assertEquals(0, msg.getStreamPartition());
    assertEquals(1528228173462L, msg.getTimestamp());
    assertEquals(new Date(1528228173462L), msg.getTimestampAsDate());
    assertEquals(0, msg.getSequenceNumber());
    assertEquals(PUBLISHER_ID, msg.getPublisherId());
    assertEquals("1", msg.getMsgChainId());
    assertEquals(null, msg.getPreviousMessageRef());
    assertEquals(StreamMessage.MessageType.STREAM_MESSAGE, msg.getMessageType());
    assertEquals(StreamMessage.Content.Type.JSON, msg.getContentType());
    assertEquals(StreamMessage.EncryptionType.NONE, msg.getEncryptionType());
    assertTrue(msg.getParsedContent() instanceof Map);
    assertEquals("2", msg.getParsedContent().get("desi"));
    assertEquals(StreamMessage.SignatureType.NONE, msg.getSignatureType());
    assertEquals(null, msg.getSignature());
  }
}
