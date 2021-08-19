package com.streamr.client.protocol.control_layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.streamr.client.protocol.common.MessageRef;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResendFromRequestAdapterTest {
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
            "[2,12,\"requestId\",\"streamId\",0,[143415425455,0],\"publisherId\",\"sessionToken\"]",
            new ResendFromRequest(
                "requestId",
                "streamId",
                0,
                new MessageRef(143415425455L, 0L),
                "publisherId",
                "sessionToken")),
        arguments(
            "[2,12,\"requestId\",\"streamId\",0,[143415425455,0],null,null]",
            new ResendFromRequest(
                "requestId", "streamId", 0, new MessageRef(143415425455L, 0L), null)));
  }
}
