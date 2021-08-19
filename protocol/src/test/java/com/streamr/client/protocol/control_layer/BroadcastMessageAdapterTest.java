package com.streamr.client.protocol.control_layer;

import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorld;
import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorldSerialized32;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class BroadcastMessageAdapterTest {
  @Test
  void serializationAndDeserialization() throws IOException {
    String serializedMessage = String.format("[2,0,\"requestId\",%s]", helloWorldSerialized32);
    ControlMessage message = new BroadcastMessage("requestId", helloWorld);
    assertEquals(message, ControlMessage.fromJson(serializedMessage));
    assertEquals(serializedMessage, message.toJson());
  }
}
