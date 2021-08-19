package com.streamr.client.protocol.message_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GroupKeyErrorResponseAdapterTest {
  @Test
  void serializationAndDeserialization() {
    String serializedMessage =
        "[\"requestId\",\"streamId\",\"errorCode\",\"errorMessage\",[\"groupKey1\",\"groupKey2\"]]";
    GroupKeyErrorResponse message =
        new GroupKeyErrorResponse(
            "requestId",
            "streamId",
            "errorCode",
            "errorMessage",
            Arrays.asList("groupKey1", "groupKey2"));
    AbstractGroupKeyMessage deserialize =
        AbstractGroupKeyMessage.deserialize(
            serializedMessage, StreamMessage.MessageType.GROUP_KEY_ERROR_RESPONSE);
    assertEquals(message, deserialize);
    assertEquals(serializedMessage, message.serialize());
  }
}
