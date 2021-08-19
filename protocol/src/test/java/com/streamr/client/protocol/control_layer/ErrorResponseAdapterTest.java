package com.streamr.client.protocol.control_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ErrorResponseAdapterTest {
  @Test
  void serializationAndDeserialization() throws IOException {
    String serializedMessage = "[2,7,\"requestId\",\"errorMessage\",\"ERROR_CODE\"]";
    ControlMessage message = new ErrorResponse("requestId", "errorMessage", "ERROR_CODE");
    assertEquals(message, ControlMessage.fromJson(serializedMessage));
    assertEquals(serializedMessage, message.toJson());
  }
}
