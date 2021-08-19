package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GroupKeyRequestAdapterTest {
  @Test
  void serializationAndDeserialization() {
    String serializedMessage =
        "[\"requestId\",\"streamId\",\"rsaPublicKey\",[\"groupKey1\",\"groupKey2\"]]";
    GroupKeyRequest message =
        new GroupKeyRequest(
            "requestId", "streamId", "rsaPublicKey", Arrays.asList("groupKey1", "groupKey2"));
    AbstractGroupKeyMessage deserialize =
        AbstractGroupKeyMessage.deserialize(
            serializedMessage, StreamMessage.MessageType.GROUP_KEY_REQUEST);
    assertEquals(message, deserialize);
    assertEquals(serializedMessage, message.serialize());
  }
}
