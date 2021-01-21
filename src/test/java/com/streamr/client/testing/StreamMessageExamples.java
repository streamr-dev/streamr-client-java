package com.streamr.client.testing;

import com.streamr.client.protocol.message_layer.Json;
import com.streamr.client.protocol.message_layer.MessageId;
import com.streamr.client.protocol.message_layer.MessageRef;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.utils.Address;
import java.util.HashMap;
import java.util.Map;

public class StreamMessageExamples {
  public static class InvalidSignature {
    public static final StreamMessage helloWorld;

    static {
      final Address publisherId = new Address("publisherId");
      final MessageId id =
          new MessageId("7wa7APtlTq6EC5iTCBy6dw", 0, 1528228173462L, 0, publisherId, "1");

      final Map<String, Object> content = new HashMap<>();
      content.put("hello", "world");
      final String serializedContent = Json.mapAdapter.toJson(content);

      helloWorld =
          new StreamMessage.Builder()
              .withMessageId(id)
              .withPreviousMessageRef(new MessageRef(1528228170000L, 0))
              .withMessageType(StreamMessage.MessageType.STREAM_MESSAGE)
              .withSerializedContent(serializedContent)
              .withEncryptionType(StreamMessage.EncryptionType.NONE)
              .withGroupKeyId(null)
              .withSignatureType(StreamMessage.SignatureType.ETH)
              .withSignature("signature")
              .createStreamMessage();
    }

    public static final String helloWorldSerialized31 =
        "[31,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherid\",\"1\"],[1528228170000,0],27,0,\"{\\\"hello\\\":\\\"world\\\"}\",2,\"signature\"]";
    public static final String helloWorldSerialized32 =
        "[32,[\"7wa7APtlTq6EC5iTCBy6dw\",0,1528228173462,0,\"publisherid\",\"1\"],[1528228170000,0],27,0,0,null,\"{\\\"hello\\\":\\\"world\\\"}\",null,2,\"signature\"]";
  }
}
