package com.streamr.client.protocol.control_layer;

import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorld;
import static com.streamr.client.testing.StreamMessageExamples.InvalidSignature.helloWorldSerialized32;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UnicastMessageAdapterTest {
  @ParameterizedTest(name = "serialize and deserialize {0}")
  @MethodSource("testDataProvider")
  void serializationAndDeserialization(String serializedMessage, ControlMessage message)
      throws IOException {
    assertEquals(message, ControlMessage.fromJson(serializedMessage));
    assertEquals(serializedMessage, message.toJson());
  }

  static Stream<Arguments> testDataProvider() {
    return Stream.of(
        arguments(
            String.format("[2,1,\"requestId\",%s]", helloWorldSerialized32),
            new UnicastMessage("requestId", helloWorld)));
  }
}
